package org.silkframework.runtime.activity

import java.util.concurrent.atomic.AtomicBoolean

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.users.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActivityExecutionTest extends FlatSpec with MustMatchers {
  behavior of "Activity Execution"

  private val testUser = new User {
    override def uri = "urn:user:user1"
  }
  implicit val userContext: UserContext = new UserContext {
    def user: Option[User] = Some(testUser)
  }

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

  it should "maintain parallelism if activities are blocking" in {
    val parallism = Activity.forkJoinPool.getParallelism

    val blockingActivities =
      for(i <- 0 until parallism) yield {
        val running = new AtomicBoolean(false)
        Activity(new BlockingActivity(running)).start()
        running
      }

    val sleepingActivities =
      for(i <- 0 until (parallism - 1)) yield {
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