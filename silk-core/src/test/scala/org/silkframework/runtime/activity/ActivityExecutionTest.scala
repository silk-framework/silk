package org.silkframework.runtime.activity

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}

import org.silkframework.runtime.activity.Status.{Finished, Running, Waiting}
import org.silkframework.runtime.users.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ActivityExecutionTest extends AnyFlatSpec with Matchers with Eventually  {
  behavior of "Activity Execution"

  private val testUser = new User {
    override def uri = "urn:user:user1"
  }
  implicit val userContext: UserContext = new UserContext {
    def user: Option[User] = Some(testUser)
    override def executionContext: UserExecutionContext = UserExecutionContext()

    override def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext = this
  }
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(30, Seconds)))

  private val parallelism = ActivityExecution.forkJoinPool.getParallelism

  it should "interrupt activities when they are cancelled by the user" in {
    val activityExecution = new ActivityExecution(new SleepingActivity(), projectAndTaskId = None)
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

  it should "start and startBlocking should both be run in the same pool" in {
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