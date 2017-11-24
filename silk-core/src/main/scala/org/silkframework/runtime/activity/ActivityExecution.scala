package org.silkframework.runtime.activity

import java.util.concurrent.ForkJoinTask

import org.silkframework.runtime.activity.Status.{Canceling, Finished}

import scala.util.control.NonFatal

private class ActivityExecution[T](activity: Activity[T],
                                   parent: Option[ActivityContext[_]] = None,
                                   progressContribution: Double = 0.0) extends ActivityMonitor[T](activity.name, parent, progressContribution, activity.initialValue)
    with ActivityControl[T] {

  /**
    * The name of the activity.
    */
  override val name: String = activity.name

  @volatile
  private var user: UserContext = UserContext.Empty

  @volatile
  private var forkJoinRunner: Option[ForkJoinRunner] = None

  @volatile
  private var startTimestamp: Option[Long] = None

  override def startTime: Option[Long] = startTimestamp

  override def start()(implicit user: UserContext): Unit = {
    // Check if the current activity is still running
    if (status().isRunning) {
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    }
    // Execute activity
    this.user = user
    status.update(Status.Started())
    val forkJoin = new ForkJoinRunner()
    forkJoinRunner = Some(forkJoin)
    if (parent.isDefined) {
      forkJoin.fork()
    } else {
      Activity.forkJoinPool.execute(forkJoin)
    }
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    this.user = user
    status.update(Status.Started())
    runActivity()
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    this.user = user
    status.update(Status.Started())
    for (v <- initialValue)
      value.update(v)
    runActivity()
    value()
  }

  override def cancel(): Unit = {
    if (status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      status.update(Status.Canceling(status().progress))
      children().foreach(_.cancel())
      activity.cancelExecution()
    }
  }

  override def reset(): Unit = {
    activity.initialValue.foreach(value.update)
    activity.reset()
  }

  def waitUntilFinished(): Unit = {
    for (runner <- forkJoinRunner) {
      try {
        runner.join()
      } catch {
        case NonFatal(ex) =>
          status() match {
            case Finished(false, _, Some(cause)) =>
              throw cause
            case _ =>
              throw ex
          }
      }
    }
  }

  override def underlying: Activity[T] = activity

  private def runActivity(): Unit = synchronized {
    if (!parent.exists(_.status().isInstanceOf[Canceling])) {
      startTimestamp = Some(System.currentTimeMillis)
      try {
        activity.run(this)
        status.update(Status.Finished(success = true, System.currentTimeMillis - startTimestamp.get))
      } catch {
        case ex: Throwable =>
          status.update(Status.Finished(success = false, System.currentTimeMillis - startTimestamp.get, Some(ex)))
          throw ex
      } finally {
        startTimestamp = None
        forkJoinRunner = None
      }
    }
  }


  /**
    * A fork join task that runs the activity.
    */
  private class ForkJoinRunner extends ForkJoinTask[Unit] {

    override def getRawResult = {}

    override def setRawResult(value: Unit): Unit = {}

    override def exec() = {
      runActivity()
      true
    }
  }

}