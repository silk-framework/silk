package org.silkframework.runtime.activity

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Running, Waiting}
import org.silkframework.runtime.users.{User, UserActions}

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ActivityExecutionTest extends AnyFlatSpec with Matchers with Eventually  {
  behavior of "Activity Execution"

  private val testUser = new User {
    override def uri = "urn:user:user1"
    override def groups: Set[String] = Set.empty
    override def actions: UserActions = UserActions.all
  }
  implicit val userContext: UserContext = new UserContext {
    def user: Option[User] = Some(testUser)
    override def executionContext: UserExecutionContext = UserExecutionContext()

    override def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext = this
  }
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(30, Seconds)))

  private val parallelism = ActivityExecution.forkJoinPool.getParallelism

  it should "interrupt activities when they are cancelled by the user" in {
    val activityExecution = Activity(new SleepingActivity())
    val start = System.currentTimeMillis()
    Future {
      while(!activityExecution.value()) {
        val SHORT_TIME = 50
        Thread.sleep(SHORT_TIME)
      }
      activityExecution.cancel()
    }
    activityExecution.startBlocking()
    val passedTime = System.currentTimeMillis() - start
    val STILL_SHORT_TIME = 1000L
    passedTime must be < STILL_SHORT_TIME
    activityExecution.queueTime mustBe defined
    activityExecution.startTime mustBe defined
    activityExecution.lastResult mustBe defined
    val result = activityExecution.lastResult.get
    result.metaData.startedByUser mustBe Some(testUser)
    result.metaData.cancelledBy mustBe Some(testUser)
  }

  it should "put activities into waiting if all thread pool slots are occupied" in {
    // Start one more activity than the thread pool allowed
    val sleepingActivities =
      for (_ <- 0 until (parallelism + 1)) yield {
        val activity = Activity(new SleepingActivity())
        activity.start()
        activity
      }

    // Wait until all activities except the last one are running
    for(activity <- sleepingActivities.init) {
      eventually { activity.status() must not be a[Waiting] }
    }

    // Make sure that the last activity is not executed yet
    eventually {
      sleepingActivities.last.status() mustBe a[Waiting]
    }

    // Clean up: cancel all activities
    stopActivities(sleepingActivities)
  }

  it should "maintain parallelism if activities are blocking" in {
    val blockingActivities =
      for(_ <- 0 until parallelism) yield {
        val activity = Activity(new BlockingActivity())
        activity.start()
        activity
      }

    val sleepingActivities =
      for(_ <- 0 until (parallelism - 1)) yield {
        val activity = Activity(new SleepingActivity())
        activity.start()
        activity
      }

    eventually {
      blockingActivities.forall(_.value()) mustBe true
      sleepingActivities.forall(_.value()) mustBe true
    }

    // Only stop the blocking activities
    for (activity <- blockingActivities) {
      activity.cancel()
    }

    // The sleeping activities should still be running
    // This check is needed because the call to blockUntil might execute a sleeping activity internally
    // In this case the sleeping activity should not be cancelled even though it's running in the same thread.
    sleepingActivities.forall(_.status().isInstanceOf[Running]) mustBe true

    // Clean up
    stopActivities(sleepingActivities)

    // Make sure that the blocking activities have been stopped as well now
    eventually {
      for (activity <- blockingActivities) {
        activity.status() match {
          case Finished(_, _, cancelled, _) =>
            cancelled mustBe true
          case status: Status =>
            fail("Unexpected status: " + status)
        }
      }
    }
  }

  it should "allow activities to skip the waiting queue" in {
    val sleepingActivities =
      for (_ <- 0 until parallelism) yield {
        val activity = Activity(new SleepingActivity())
        activity.start()
        activity
      }

    // Activities that have already been started should skip the waiting queue
    val priorityActivity1 = Activity(new SleepingActivity())
    priorityActivity1.start()
    eventually {
      priorityActivity1.status() mustBe a[Waiting]
    }
    priorityActivity1.startPrioritized()
    eventually {
      priorityActivity1.value() mustBe true
    }

    // Activities that have not been started yet, should be started immediately
    val priorityActivity2 = Activity(new SleepingActivity())
    priorityActivity2.startPrioritized()
    eventually {
      priorityActivity2.value() mustBe true
    }

    stopActivities(sleepingActivities :+ priorityActivity1 :+ priorityActivity2)
  }

  it should "run startBlocking in the same pool as start" in {
    // Start some activities and only leave one free slot in the activity pool
    val sleepingActivitiesAsync =
      for (_ <- 0 until (parallelism - 1)) yield {
        val activity = Activity(new SleepingActivity())
        activity.start()
        activity
      }

    for (activity <- sleepingActivitiesAsync) {
      eventually {
        activity.status().isRunning mustBe true
      }
    }

    // Start two activities using startBlocking
    val sleepingActivitiesSync =
      for (i <- 0 until 2) yield {
        val activity = Activity(new SleepingActivity())
        Future {
          activity.startBlocking()
        }
        if(i == 0) {
          // Only the first one will find a free slot
          eventually {
            activity.status().isRunning mustBe true
          }
        } else {
          // The second one should be in waiting and not execute
          Thread.sleep(500)
          activity.status() mustBe a[Waiting]
        }
        activity
      }

    // Clean up: cancel all activities
    stopActivities(sleepingActivitiesAsync)
    stopActivities(sleepingActivitiesSync)
  }

  it should "start an activity that is not running when startOrReRun is called" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter)
    gated.released = true // do not block, so the run finishes immediately
    val activity = Activity(gated)
    activity.startOrReRun()
    activity.waitUntilFinished()
    counter.get() mustBe 1
    activity.status() mustBe a[Finished]
    // The recorded result must carry the terminal finish status, never a status left by a concurrent restart.
    activity.lastResult.get.metaData.finishStatus.get mustBe a[Finished]
  }

  it should "re-run an activity when startOrReRun is called while it is still running" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { activity.status().isRunning mustBe true }
    eventually { counter.get() mustBe 1 } // first run is in progress (blocked)
    // Request another run while the first run is still blocked.
    activity.startOrReRun()
    // Release the gate: the first run finishes, then the requested re-run must happen.
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
  }

  it should "coalesce multiple startOrReRun requests during a single run into exactly one additional run" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { activity.status().isRunning mustBe true }
    eventually { counter.get() mustBe 1 }
    // Several requests during the same run should result in only one extra run.
    activity.startOrReRun()
    activity.startOrReRun()
    activity.startOrReRun()
    gated.released = true
    eventually { activity.status() mustBe a[Finished] }
    counter.get() mustBe 2
  }

  it should "discard a pending re-run request when the activity is cancelled" in {
    val counter = new AtomicInteger(0)
    val gated = new StubbornGatedActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    // Request a re-run first, then cancel: the cancellation discards the earlier request.
    activity.startOrReRun()
    activity.cancel()
    gated.released = true
    eventually { activity.status() mustBe a[Finished] }
    counter.get() mustBe 1
  }

  it should "execute a re-run that is requested after the running activity has been cancelled" in {
    val counter = new AtomicInteger(0)
    val gated = new StubbornGatedActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    // Cancel first, then request a run: the request postdates the cancellation and must be honored,
    // even though the current run finishes cancelled.
    activity.cancel()
    activity.status().isRunning mustBe true // the stubborn activity ignores the cancellation until released
    activity.startOrReRun()
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
  }

  it should "not record the previous run's cancellation on a coalesced re-run" in {
    val counter = new AtomicInteger(0)
    val gated = new StubbornGatedActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    // Cancel the running activity, then request a re-run that postdates the cancellation and must be honored.
    activity.cancel()
    activity.startOrReRun()
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
    // The successful re-run must not inherit the earlier run's cancellation metadata, and its recorded finish
    // status must be the terminal one.
    val metaData = activity.lastResult.get.metaData
    metaData.cancelledBy mustBe None
    metaData.cancelledAt mustBe None
    metaData.finishStatus.get mustBe a[Finished]
    val finished = metaData.finishStatus.get.asInstanceOf[Finished]
    finished.success mustBe true
    finished.cancelled mustBe false
  }

  it should "execute a re-run requested after a cancelled run that exits via thread interruption" in {
    val counter = new AtomicInteger(0)
    val gated = new InterruptibleActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    // Cancel the first run, then let it exit via InterruptedException only after the re-run has been requested,
    // so the request postdates the cancellation and must be honored even though the run failed by interruption.
    activity.cancel()
    eventually { gated.interruptCaught mustBe true }
    activity.startOrReRun()
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
  }

  it should "not let a delayed cancellation abort a re-run requested after it" in {
    val counter = new AtomicInteger(0)
    val gated = new CancelDeliveryTestActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 } // first run in progress
    // Cancel, but block the cancellation mid-delivery (inside cancelExecution) to simulate it being preempted.
    Future { activity.cancel() }
    eventually { gated.cancelExecutionEntered mustBe true }
    // Request a re-run that postdates the cancellation; per contract it must survive.
    activity.startOrReRun()
    // Let the first run finish (the re-run must wait for the in-flight cancellation to drain), then let the delayed
    // cancellation finish delivering. It must not touch the re-run.
    gated.releaseRun1 = true
    gated.releaseCancel = true
    eventually { gated.run2Started mustBe true }
    gated.releaseRun2 = true
    eventually { gated.run2Completed mustBe true }
    eventually { activity.status() mustBe a[Finished] }
  }

  it should "reach a terminal state when it is cancelled before the worker thread picks it up" in {
    val blockers = occupyAllPoolSlots()
    try {
      val counter = new AtomicInteger(0)
      val gated = new GatedActivity(counter)
      gated.released = true // would run to completion immediately if it were ever executed
      val activity = Activity(gated)
      activity.start()
      eventually { activity.status() mustBe a[Waiting] }
      // Cancel while still queued: sets the cancel flag before any worker calls markRunning().
      activity.cancel()
      stopActivities(blockers) // free the pool so a worker picks up the cancelled, still-queued activity
      activity.waitUntilFinished()
      // The body must have been skipped, and the activity must reach a terminal Finished state rather than
      // stay stuck in a non-terminal state (which would leave it permanently un-restartable via startOrReRun).
      counter.get() mustBe 0
      eventually { activity.status() mustBe a[Finished] }
    } finally {
      stopActivities(blockers)
    }
  }

  it should "not schedule a redundant re-run when startOrReRun is called while the activity is only queued" in {
    val blockers = occupyAllPoolSlots()
    try {
      val counter = new AtomicInteger(0)
      val gated = new GatedActivity(counter)
      gated.released = true // each run finishes immediately once it starts
      val activity = Activity(gated)
      activity.start()
      eventually { activity.status() mustBe a[Waiting] }
      // Called while still queued: the pending run already covers it, so no extra run should be scheduled.
      activity.startOrReRun()
      stopActivities(blockers) // free the pool so the queued run executes
      activity.waitUntilFinished()
      counter.get() mustBe 1
      activity.status() mustBe a[Finished]
    } finally {
      stopActivities(blockers)
    }
  }

  it should "execute a requested re-run even if the current run fails" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter, failFirstRun = true)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.startOrReRun()
    // Release the gate: the first run fails, but the requested re-run must still be executed.
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually {
      // The final status must reflect the successful re-run, not the failed first run
      val status = activity.status()
      status mustBe a[Finished]
      val finished = status.asInstanceOf[Finished]
      finished.success mustBe true
      finished.cancelled mustBe false
      finished.exception mustBe None
    }
  }

  it should "keep control operations responsive while a cancellation delivery is still in flight" in {
    val counter = new AtomicInteger(0)
    val gated = new CancelDeliveryTestActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    // Block the cancellation mid-delivery, then let the first run finish.
    Future { activity.cancel() }
    eventually { gated.cancelExecutionEntered mustBe true }
    gated.releaseRun1 = true
    eventually { activity.status() mustBe a[Finished] }
    // A fresh start and further control operations must not block on the in-flight delivery.
    val startMillis = System.currentTimeMillis()
    activity.start()
    activity.startOrReRun()
    (System.currentTimeMillis() - startMillis) must be < 5000L
    // The new run's body must wait for the delivery to drain.
    Thread.sleep(200)
    gated.run2Started mustBe false
    // Release the delivery: the new run must execute without inheriting the old cancellation.
    gated.releaseCancel = true
    gated.releaseRun2 = true
    eventually { gated.run2Completed mustBe true }
    eventually { activity.status() mustBe a[Finished] }
  }

  it should "honor a cancellation of a new run while a previous run's cancellation is still in flight" in {
    val counter = new AtomicInteger(0)
    val gated = new CancelDeliveryTestActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    Future { activity.cancel() }
    eventually { gated.cancelExecutionEntered mustBe true }
    gated.releaseRun1 = true
    eventually { activity.status() mustBe a[Finished] }
    // Start a new run (it waits for the old delivery) and cancel it; the cancellation must not be wiped.
    activity.start()
    Future { activity.cancel() } // blocks in cancelExecution until released
    eventually { activity.status() mustBe a[Canceling] }
    gated.releaseCancel = true
    eventually { gated.wasCancelled() mustBe true }
    gated.releaseRun2 = true
    eventually {
      activity.status() match {
        case Finished(_, _, cancelled, _) => cancelled mustBe true
        case status: Status => fail("Unexpected status: " + status)
      }
    }
    gated.run2Completed mustBe false
  }

  it should "publish the failure of a run before executing its requested re-run" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter, failFirstRun = true)
    val activity = Activity(gated)
    val observedStatuses = new ConcurrentLinkedQueue[Status]()
    val observer: Status => Unit = observedStatuses.add(_) // strong reference, subscribers are weakly referenced
    activity.status.subscribe(observer)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.startOrReRun()
    gated.released = true
    eventually {
      counter.get() mustBe 2
      activity.status() match {
        case Finished(success, _, _, exception) =>
          success mustBe true
          exception mustBe None
        case status: Status => fail("Unexpected status: " + status)
      }
    }
    // The first run's failure must have been published before the re-run's success.
    val statuses = observedStatuses.asScala.toSeq
    val failureIndex = statuses.indexWhere {
      case Finished(false, _, _, Some(_)) => true
      case _ => false
    }
    failureIndex must be >= 0
    statuses.last match {
      case Finished(success, _, _, _) => success mustBe true
      case status: Status => fail("Unexpected status: " + status)
    }
  }

  it should "not publish a failure for a cancelled run that coalesces into a re-run" in {
    val counter = new AtomicInteger(0)
    val gated = new InterruptibleActivity(counter)
    val activity = Activity(gated)
    val observedStatuses = new ConcurrentLinkedQueue[Status]()
    val observer: Status => Unit = observedStatuses.add(_) // strong reference, subscribers are weakly referenced
    activity.status.subscribe(observer)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.cancel()
    eventually { gated.interruptCaught mustBe true }
    activity.startOrReRun()
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
    // A cancellation exit is not a failure and must not be published as one.
    for (finished <- observedStatuses.asScala.collect { case f: Finished => f }) {
      finished.success mustBe true
    }
  }

  it should "report Running and stay cancellable during a coalesced re-run after a cancelled run" in {
    val counter = new AtomicInteger(0)
    val gated = new TwoGateBlockUntilActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.cancel()
    activity.startOrReRun() // postdates the cancellation, must be honored
    gated.release1 = true
    // The re-run must not stay stuck in the previous run's Canceling state.
    eventually {
      counter.get() mustBe 2
      activity.status() mustBe a[Running]
    }
    // The re-run must be cancellable.
    activity.cancel()
    eventually {
      activity.status() match {
        case Finished(_, _, cancelled, _) => cancelled mustBe true
        case status: Status => fail("Unexpected status: " + status)
      }
    }
  }

  it should "not exit blockUntil prematurely during a coalesced re-run after a cancelled run" in {
    val counter = new AtomicInteger(0)
    val gated = new TwoGateBlockUntilActivity(counter)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.cancel()
    activity.startOrReRun()
    gated.release1 = true
    eventually { counter.get() mustBe 2 }
    // Give blockUntil time to poll at least once (500ms interval); it must keep blocking.
    Thread.sleep(700)
    gated.run2ExitedEarly mustBe false
    gated.release2 = true
    eventually {
      activity.status() match {
        case Finished(success, _, cancelled, _) =>
          success mustBe true
          cancelled mustBe false
        case status: Status => fail("Unexpected status: " + status)
      }
    }
    gated.run2ExitedEarly mustBe false
  }

  it should "execute child activities during a coalesced re-run after a cancelled run" in {
    val counter = new AtomicInteger(0)
    val gated = new TwoGateBlockUntilActivity(counter, runChild = true)
    val activity = Activity(gated)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.cancel()
    activity.startOrReRun()
    gated.release1 = true
    eventually { counter.get() mustBe 2 }
    gated.release2 = true
    eventually { activity.status() mustBe a[Finished] }
    gated.childRan mustBe true
  }

  it should "skip the body of a run that is cancelled before pickup even while the cancellation is still delivering" in {
    val blockers = occupyAllPoolSlots()
    val counter = new AtomicInteger(0)
    val gated = new CancelDeliveryTestActivity(counter)
    try {
      val activity = Activity(gated)
      activity.start()
      eventually { activity.status() mustBe a[Waiting] }
      Future { activity.cancel() } // blocks mid-delivery inside cancelExecution, so the cancel flag is not set yet
      eventually { gated.cancelExecutionEntered mustBe true }
      stopActivities(blockers) // free the pool so a worker picks up the cancelled, still-queued activity
      // The body must be skipped although the cancel flag has not been delivered yet.
      eventually { activity.status() mustBe a[Finished] }
      counter.get() mustBe 0
      activity.status().asInstanceOf[Finished].cancelled mustBe true
    } finally {
      gated.releaseCancel = true // let the blocked cancellation finish delivering
      stopActivities(blockers)
    }
  }

  it should "record the execution result even if a status observer fails during the finish transition" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter)
    gated.released = true
    val activity = Activity(gated)
    val observer: Status => Unit = {
      case _: Finished => throw new RuntimeException("Intentional observer failure")
      case _ =>
    }
    activity.status.subscribe(observer)
    activity.start()
    eventually { activity.status() mustBe a[Finished] }
    // The terminal result must be recorded although the observer failed during every finish publish.
    eventually { activity.lastResult mustBe defined }
    activity.lastResult.get.metaData.finishStatus.get mustBe a[Finished]
  }

  it should "publish a genuine failure that races with a cancellation before executing the requested re-run" in {
    val counter = new AtomicInteger(0)
    val gated = new FailingStubbornActivity(counter)
    val activity = Activity(gated)
    val observedStatuses = new ConcurrentLinkedQueue[Status]()
    val observer: Status => Unit = observedStatuses.add(_) // strong reference, subscribers are weakly referenced
    activity.status.subscribe(observer)
    activity.start()
    eventually { counter.get() mustBe 1 }
    activity.cancel()
    activity.startOrReRun() // postdates the cancellation, must be honored
    gated.released = true // the first run now exits with a RuntimeException instead of a normal cancellation exit
    eventually { counter.get() mustBe 2 }
    eventually {
      val status = activity.status()
      status mustBe a[Finished]
      status.asInstanceOf[Finished].success mustBe true
    }
    // The genuine failure must have been published even though it raced with the cancellation.
    val failure = observedStatuses.asScala.collectFirst { case f @ Finished(false, _, _, Some(_)) => f }
    failure mustBe defined
    failure.get.cancelled mustBe true
  }

  it should "execute a coalesced re-run on behalf of the user who requested it" in {
    val counter = new AtomicInteger(0)
    val gated = new UserRecordingActivity(counter)
    val activity = Activity(gated)
    activity.start() // started by user1 (class-level implicit)
    eventually { counter.get() mustBe 1 }
    activity.startOrReRun()(userContextFor("urn:user:user2"))
    gated.released = true
    eventually { counter.get() mustBe 2 }
    eventually { activity.status() mustBe a[Finished] }
    // The re-run must execute under the requesting user's context and be attributed to that user.
    gated.usersSeen.asScala.toSeq.map(_.user.map(_.uri)) mustBe Seq(Some("urn:user:user1"), Some("urn:user:user2"))
    activity.startedBy.user.map(_.uri) mustBe Some("urn:user:user2")
    activity.lastResult.get.metaData.startedByUser.map(_.uri) mustBe Some("urn:user:user2")
  }

  it should "propagate the failure of the initial run to a blocking caller even if a requested re-run succeeds" in {
    val counter = new AtomicInteger(0)
    val gated = new GatedActivity(counter, failFirstRun = true)
    val activity = Activity(gated)
    Future {
      eventually { counter.get() mustBe 1 }
      activity.startOrReRun()
      gated.released = true
    }
    // The blocking start must report the failure of the run it initiated, not the success of the coalesced re-run.
    val ex = intercept[RuntimeException] {
      activity.startBlocking()
    }
    ex.getMessage must include("Intentional test failure")
    eventually { counter.get() mustBe 2 }
    // The activity itself finished with the successful re-run.
    eventually {
      val status = activity.status()
      status mustBe a[Finished]
      status.asInstanceOf[Finished].success mustBe true
    }
  }

  private def userContextFor(userUri: String): UserContext = new UserContext {
    private val contextUser = new User {
      override def uri: String = userUri
      override def groups: Set[String] = Set.empty
      override def actions: UserActions = UserActions.all
    }
    def user: Option[User] = Some(contextUser)
    override def executionContext: UserExecutionContext = UserExecutionContext()
    override def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext = this
  }

  // Fills every pool slot so a subsequently started activity stays queued (Waiting). Free again with stopActivities().
  private def occupyAllPoolSlots(): Seq[ActivityControl[Boolean]] = {
    for (_ <- 0 until parallelism) yield {
      val activity = Activity(new SleepingActivity())
      activity.start()
      activity
    }
  }

  private def stopActivities(activities: Iterable[ActivityControl[_]]): Unit = {
    for (activity <- activities) {
      activity.cancel()
    }
    for (activity <- activities) {
      activity.waitUntilFinished()
    }
  }
}

/**
  * Activity that sleeps for a long time.
  * Will set the boolean context value to true as soon as it's being executed.
  */
class SleepingActivity() extends Activity[Boolean] {

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    context.value() = true
    val LONG_TIME = 100000
    Thread.sleep(LONG_TIME)
  }
}

/**
  * Activity that just blocks.
  * Will set the boolean context value to true as soon as it's being executed.
  */
class BlockingActivity() extends Activity[Boolean] {

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    context.value() = true
    context.blockUntil(() => false)
  }
}

/**
  * Activity that counts its runs and blocks each run until `released` is set to true.
  * Optionally fails its first run after being released.
  * Used to test [[ActivityControl.startOrReRun]].
  */
class GatedActivity(counter: AtomicInteger, failFirstRun: Boolean = false) extends Activity[Boolean] {

  @volatile
  var released: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    val runNumber = counter.incrementAndGet()
    context.value() = true
    context.blockUntil(() => released)
    if(failFirstRun && runNumber == 1) {
      throw new RuntimeException("Intentional test failure of the first run")
    }
  }
}

/**
  * Like [[GatedActivity]], but ignores cancellation and thread interrupts: each run blocks until `released` is set to
  * true. Used to test the interaction of [[ActivityControl.cancel]] and [[ActivityControl.startOrReRun]] while a run
  * is guaranteed to be still in progress.
  */
class StubbornGatedActivity(counter: AtomicInteger) extends Activity[Boolean] {

  @volatile
  var released: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    counter.incrementAndGet()
    context.value() = true
    while(!released) {
      try {
        Thread.sleep(10)
      } catch {
        case _: InterruptedException => // Keep blocking until released, even when cancelled
      }
    }
  }
}

/**
  * Activity whose first run exits via InterruptedException once cancelled, but only after `released` is set, so a test
  * can request a re-run before the interruption propagates. Later runs finish immediately. Used to test that a re-run
  * requested after a cancelled, interrupt-terminated run is still honored.
  */
class InterruptibleActivity(counter: AtomicInteger) extends Activity[Boolean] {

  @volatile
  var released: Boolean = false

  @volatile
  var interruptCaught: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    val runNumber = counter.incrementAndGet()
    context.value() = true
    if (runNumber == 1) {
      try {
        while (true) {
          Thread.sleep(10)
        }
      } catch {
        case e: InterruptedException =>
          interruptCaught = true
          while (!released) {
            try Thread.sleep(10) catch { case _: InterruptedException => }
          }
          throw e
      }
    }
  }
}

/**
  * Two-gate activity for testing coalesced re-runs after a cancellation. Run 1 ignores interrupts and blocks until
  * `release1`. Run 2 optionally runs a child activity, then waits via context.blockUntil until `release2`.
  */
class TwoGateBlockUntilActivity(counter: AtomicInteger, runChild: Boolean = false) extends Activity[Boolean] {

  @volatile var release1: Boolean = false
  @volatile var release2: Boolean = false
  @volatile var run2ExitedEarly: Boolean = false // set if run 2 passes blockUntil while release2 is still false
  @volatile var childRan: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    val runNumber = counter.incrementAndGet()
    context.value() = true
    if (runNumber == 1) {
      while (!release1) {
        try Thread.sleep(10) catch { case _: InterruptedException => } // Keep blocking until released, even when cancelled
      }
    } else {
      if (runChild) {
        context.child(new Activity[Unit] {
          override def run(childContext: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
            childRan = true
          }
        }).startBlocking()
      }
      context.blockUntil(() => release2)
      if (!release2) {
        run2ExitedEarly = true
      }
    }
  }
}

/**
  * Like [[StubbornGatedActivity]], but its first run throws a RuntimeException once released, simulating a genuine
  * failure that races with a cancellation. Later runs finish immediately.
  */
class FailingStubbornActivity(counter: AtomicInteger) extends Activity[Boolean] {

  @volatile
  var released: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    val runNumber = counter.incrementAndGet()
    context.value() = true
    if (runNumber == 1) {
      while (!released) {
        try Thread.sleep(10) catch { case _: InterruptedException => } // Keep blocking until released, even when cancelled
      }
      throw new RuntimeException("Intentional test failure racing with the cancellation")
    }
  }
}

/**
  * Like [[GatedActivity]], but additionally records the user context each run executes under.
  */
class UserRecordingActivity(counter: AtomicInteger) extends Activity[Boolean] {

  @volatile
  var released: Boolean = false

  val usersSeen = new ConcurrentLinkedQueue[UserContext]()

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    counter.incrementAndGet()
    usersSeen.add(userContext)
    context.value() = true
    context.blockUntil(() => released)
  }
}

/**
  * Activity for exercising the cancel-delivery drain. Its first run ignores interrupts and blocks until `releaseRun1`;
  * a later run blocks until `releaseRun2` and records completion in `run2Completed` unless it was cancelled.
  * cancelExecution() signals `cancelExecutionEntered` and then blocks until `releaseCancel`, letting a test hold a
  * cancellation mid-delivery while a re-run is requested and started.
  */
class CancelDeliveryTestActivity(counter: AtomicInteger) extends Activity[Boolean] {

  @volatile var releaseRun1: Boolean = false
  @volatile var releaseRun2: Boolean = false
  @volatile var releaseCancel: Boolean = false
  @volatile var cancelExecutionEntered: Boolean = false
  @volatile var run2Started: Boolean = false
  @volatile var run2Completed: Boolean = false

  override def initialValue: Option[Boolean] = Some(false)

  override def run(context: ActivityContext[Boolean])(implicit userContext: UserContext): Unit = {
    val runNumber = counter.incrementAndGet()
    context.value() = true
    if (runNumber == 1) {
      while (!releaseRun1) sleepIgnoringInterrupt()
    } else {
      run2Started = true
      while (!releaseRun2) sleepIgnoringInterrupt()
      // A cancellation leaking into this re-run would set the cancel flag and prevent completion.
      if (!wasCancelled()) {
        run2Completed = true
      }
    }
  }

  override def cancelExecution()(implicit userContext: UserContext): Unit = {
    cancelExecutionEntered = true
    while (!releaseCancel) sleepIgnoringInterrupt()
    super.cancelExecution()
  }

  private def sleepIgnoringInterrupt(): Unit = {
    try Thread.sleep(10) catch { case _: InterruptedException => }
  }
}