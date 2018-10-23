package org.silkframework.runtime.activity

import org.scalatest.{FlatSpec, MustMatchers, ShouldMatchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActivityExecutionTest extends FlatSpec with MustMatchers {
  behavior of "Activity Execution"

  implicit val userContext: UserContext = UserContext.Empty

  it should "interrupt activities when they are cancelled by the user" in {
    val activityExecution = new ActivityExecution(new SleepingActivity())
    val start = System.currentTimeMillis()
    Future {
      val SHORT_TIME = 50
      Thread.sleep(SHORT_TIME)
      activityExecution.cancel()
    }
    activityExecution.startBlocking()
    val passedTime = System.currentTimeMillis() - start
    val STILL_SHORT_TIME = 1000L
    passedTime must be < STILL_SHORT_TIME
  }
}

class SleepingActivity() extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    val LONG_TIME = 100000
    Thread.sleep(LONG_TIME)
  }
}