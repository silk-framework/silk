package de.fuberlin.wiwiss.silk.runtime.activity

import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.runtime.oldtask.{TaskFinished, TaskStarted, TaskCanceling}

import scala.concurrent.ExecutionContext

private class ActivityExecution(task: Activity, parent: Option[ActivityContext] = None, progressContribution: Double = 0.0) extends Runnable with ActivityControl with ActivityContext {

  // TODO synchronize

  /**
   * The logger used to log status changes.
   */
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  private var statusLogLevel = Level.INFO

  /**
   * The level at which updates to the running status logged.
   * Examples are updates to the current progress or the current status message.
   */
  private var progressLogLevel = Level.INFO

  /**
   * The current status.
   */
  @volatile private var currentStatus: Status = Status.Idle()

  private var childControls: Seq[ActivityControl] = Seq.empty

  override def run(): Unit = synchronized {
    val startTime = System.currentTimeMillis
    updateStatus(Status.Started(task.taskName))

    try {
      task.run(this)
      updateStatus(Status.Finished(task.taskName, success = true, System.currentTimeMillis - startTime))
    } catch {
      case ex: Throwable =>
        logger.log(Level.WARNING, task.taskName + " failed", ex)
        updateStatus(Status.Finished(task.taskName, success = false, System.currentTimeMillis - startTime, Some(ex)))
        throw ex
    }
  }

  override def status: Status = currentStatus

  override def children(): Seq[ActivityControl] = {
    removeDoneChildren()
    childControls
  }

  override def cancel() = {
    if(status.isRunning && !status.isInstanceOf[TaskCanceling]) {
      updateStatus(Status.Canceling(task.taskName, status.progress))
      childControls.foreach(_.cancel())
      task.cancelExecution()
    }
  }

  override def updateStatus(status: Status): Unit = {
    // Log status change
    status match {
      case _: Status.Running => logger.log(progressLogLevel, status.toString)
      case _ => logger.log(statusLogLevel, status.toString)
    }

    // Advance the progress of the parent task
    for(p <- parent) {
      val progressDiff = status.progress - this.status.progress
      p.updateStatus(p.status.progress + progressDiff * progressContribution)
    }

    // Publish status change
    if(!currentStatus.isInstanceOf[Status.Canceling]) {
      currentStatus = status
      publish(status)
    }
  }

  override def executeBlocking(task: Activity, progressContribution: Double = 0.0): Unit = {
    val taskExecution = new ActivityExecution(task, Some(this), progressContribution)
    taskExecution.run()
  }

  override def executeBackground(task: Activity, progressContribution: Double = 0.0): ActivityControl = {
    val taskExecution = new ActivityExecution(task, Some(this), progressContribution)
    addChild(taskExecution)
    ExecutionContext.global.execute(taskExecution)
    taskExecution
  }

  private def addChild(control: ActivityControl): Unit = {
    childControls = childControls :+ control
  }

  private def removeDoneChildren(): Unit = {
    childControls = childControls.filter(_.status.isRunning)
  }
}