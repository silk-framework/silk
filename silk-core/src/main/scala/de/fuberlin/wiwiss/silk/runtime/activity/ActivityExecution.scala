package de.fuberlin.wiwiss.silk.runtime.activity

import java.util.logging.{Logger, Level}
import scala.concurrent.ExecutionContext

private class ActivityExecution[T](@volatile var activity: Activity[T],
                                   parent: Option[ActivityContext[_]] = None,
                                   progressContribution: Double = 0.0) extends Runnable with ActivityControl[T] with ActivityContext[T] {

  /**
   * The logger used to log status changes.
   */
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Holds the current value.
   */
  override val value = new ValueHolder[T](activity.initialValue)

  /**
   * Retrieves the logger to be used by the activity.
   */
  override val log = Logger.getLogger(activity.getClass.getName)

  /**
   * Holds the current status.
   */
  override val status = new StatusHolder(log, parent.map(_.status), progressContribution)

  // TODO synchronize
  private var childControls: Seq[ActivityControl[_]] = Seq.empty

  override def run(): Unit = synchronized {
    // Reset
    val startTime = System.currentTimeMillis
    status.update(Status.Started(activity.name))

    // Run
    try {
      activity.run(this)
      status.update(Status.Finished(activity.name, success = true, System.currentTimeMillis - startTime))
    } catch {
      case ex: Throwable =>
        logger.log(Level.WARNING, activity.name + " failed", ex)
        status.update(Status.Finished(activity.name, success = false, System.currentTimeMillis - startTime, Some(ex)))
        throw ex
    }
  }
  
  override def children(): Seq[ActivityControl[_]] = {
    removeDoneChildren()
    childControls
  }

  override def start(activity: Option[Activity[T]]): Unit = {
    // Check if the current activity is still running
    if(status().isRunning)
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    // Replace current activity
    for(a <- activity)
      this.activity = a
    // Execute activity
    ExecutionContext.global.execute(this)
  }

  override def startBlocking(initialValue: Option[T]): T = {
    for(v <- initialValue)
      value.update(v)
    run()
    value()
  }

  override def cancel() = {
    if(status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      status.update(Status.Canceling(activity.name, status().progress))
      childControls.foreach(_.cancel())
      activity.cancelExecution()
    }
  }

  override def child[R](activity: Activity[R], progressContribution: Double = 0.0): ActivityControl[R] = {
    val execution = new ActivityExecution(activity, Some(this), progressContribution)
    addChild(execution)
    execution
  }

  private def addChild(control: ActivityControl[_]): Unit = {
    childControls = childControls :+ control
  }

  private def removeDoneChildren(): Unit = {
    childControls = childControls.filter(_.status().isRunning)
  }
}