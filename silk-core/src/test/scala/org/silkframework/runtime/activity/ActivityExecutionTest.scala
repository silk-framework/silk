package org.silkframework.runtime.activity

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.activity.Status.Waiting
import org.silkframework.runtime.users.User

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActivityExecutionTest extends FlatSpec with MustMatchers with Eventually  {
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

  it should "interrupt activities when they are cancelled by the user" in {
    val running = new AtomicBoolean(false)
    val activityExecution = new ActivityExecution(new SleepingActivity(running), projectAndTaskId = None)
    val start = System.currentTimeMillis()
    Future {
      while(!running.get()) {
        val SHORT_TIME = 50
        Thread.sleep(SHORT_TIME)
      }
      activityExecution.cancel()
    }
    activityExecution.startBlocking()
    val passedTime = System.currentTimeMillis() - start
    val STILL_SHORT_TIME = 1000L
    passedTime must be < STILL_SHORT_TIME
    activityExecution.startTime mustBe defined
    activityExecution.lastResult mustBe defined
    val result = activityExecution.lastResult.get
    result.metaData.startedByUser mustBe Some(testUser)
    result.metaData.cancelledBy mustBe Some(testUser)
  }

  it should "put activities into waiting if all thread pool slots are occupied" in {
    val parallelism = Activity.forkJoinPool.getParallelism

    // Start one more activity than the thread pool allowed
    val sleepingActivities =
      for (_ <- 0 until (parallelism + 1)) yield {
        val running = new AtomicBoolean(false)
        val activity = Activity(new SleepingActivity(running))
        activity.start()
        activity
      }

    // Wait until all activities except the last one are running
    for(activity <- sleepingActivities.init) {
      eventually { activity.status() must not be a[Waiting] }
    }

    // Make sure that the last activity is not executed yet
    Thread.sleep(100)
    sleepingActivities.last.status() mustBe a[Waiting]

    // Clean up: cancel all activities
    for(activity <- sleepingActivities) {
      activity.cancel()
    }
    for (activity <- sleepingActivities) {
      activity.waitUntilFinished()
    }
  }

  it should "maintain parallelism if activities are blocking" in {
    val parallelism = Activity.forkJoinPool.getParallelism

    val blockingActivities =
      for(_ <- 0 until parallelism) yield {
        val running = new AtomicBoolean(false)
        Activity(new BlockingActivity(running)).start()
        running
      }

    val sleepingActivities =
      for(_ <- 0 until (parallelism - 1)) yield {
        val running = new AtomicBoolean(false)
        Activity(new SleepingActivity(running)).start()
        running
      }

    Thread.sleep(1000)

    blockingActivities.forall(_.get()) mustBe true
    sleepingActivities.forall(_.get()) mustBe true
  }
}

class SleepingActivity(running: AtomicBoolean) extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    running.set(true)
    val LONG_TIME = 100000
    Thread.sleep(LONG_TIME)
  }
}

class BlockingActivity(running: AtomicBoolean) extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    running.set(true)
    context.blockUntil(() => false)
  }
}