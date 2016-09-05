package org.silkframework.runtime.activity

import org.silkframework.runtime.activity.Status.Canceling

private class ActivityExecution[T](activity: Activity[T],
                                   parent: Option[ActivityContext[_]] = None,
                                   progressContribution: Double = 0.0) extends ActivityMonitor[T](activity.name, parent, progressContribution, activity.initialValue)
                                                                       with Runnable with ActivityControl[T] {

  /**
   * The name of the activity.
   */
  override val name: String = activity.name

  @volatile
  private var user: UserContext = UserContext.Empty

  override def run(): Unit = synchronized {
    if(!parent.exists(_.status().isInstanceOf[Canceling])) {
      val startTime = System.currentTimeMillis
      try {
        activity.run(this)
        status.update(Status.Finished(success = true, System.currentTimeMillis - startTime))
      } catch {
        case ex: Throwable =>
          status.update(Status.Finished(success = false, System.currentTimeMillis - startTime, Some(ex)))
          throw ex
      }
    }
  }

  override def start()(implicit user: UserContext): Unit = {
    // Check if the current activity is still running
    if(status().isRunning)
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    // Execute activity
    this.user = user
    status.update(Status.Started())
    Activity.executionContext.execute(this)
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    this.user = user
    status.update(Status.Started())
    run()
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    this.user = user
    status.update(Status.Started())
    for(v <- initialValue)
      value.update(v)
    run()
    value()
  }

  override def cancel() = {
    if(status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      status.update(Status.Canceling(status().progress))
      children().foreach(_.cancel())
      activity.cancelExecution()
    }
  }

  override def reset() = {
    activity.initialValue.foreach(value.update)
    activity.reset()
  }

  override def underlying: Activity[T] = activity

}