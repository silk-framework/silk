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
  private var startedByUser: UserContext = UserContext.Empty

  @volatile
  private var cancelledByUser: UserContext = UserContext.Empty

  @volatile
  private var forkJoinRunner: Option[ForkJoinRunner] = None

  @volatile
  private var startTimestamp: Option[Long] = None

  @volatile
  private var cancelTimestamp: Option[Long] = None

  override def startTime: Option[Long] = startTimestamp

  override def start()(implicit user: UserContext): Unit = {
    // Check if the current activity is still running
    if (status().isRunning) {
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    }
    // Execute activity
    val forkJoin = new ForkJoinRunner()
    forkJoinRunner = Some(forkJoin)
    if (parent.isDefined) {
      forkJoin.fork()
    } else {
      Activity.forkJoinPool.execute(forkJoin)
    }
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    runActivity()
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    synchronized {
      for (v <- initialValue)
        value.update(v)
      runActivity()
      value()
    }
  }

  override def cancel()(implicit user: UserContext): Unit = {
    if (status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      this.cancelledByUser = user
      this.cancelTimestamp = Some(System.currentTimeMillis())
      status.update(Status.Canceling(status().progress))
      children().foreach(_.cancel())
      activity.cancelExecution()
      lastResult = activityExecutionResult
      resetMetaData()
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

  private def runActivity()(implicit user: UserContext): Unit = synchronized {
    this.startedByUser = user
    status.update(Status.Started())
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
		lastResult = activityExecutionResult
        resetMetaData()
        forkJoinRunner = None
      }
    }
  }


  private def activityExecutionResult: ActivityExecutionResult[T] = {
    ActivityExecutionResult(
      metaData = ActivityExecutionMetaData(
        startedByUser = startedByUser.user,
        startedAt = startTimestamp,
        finishedAt = Some(System.currentTimeMillis()),
        cancelledAt = cancelTimestamp,
        cancelledBy = cancelledByUser.user,
        finishStatus = status.get
      ),
      resultValue = value.get
    )
  }

  private def resetMetaData(): Unit = {
    // Reset values
    startTimestamp = None
    startedByUser = UserContext.Empty
    cancelTimestamp = None
    cancelledByUser = UserContext.Empty
  }

  /**
    * A fork join task that runs the activity.
    */
  private class ForkJoinRunner(implicit userContext: UserContext) extends ForkJoinTask[Unit] {

    override def getRawResult: Unit = {}

    override def setRawResult(value: Unit): Unit = {}

    override def exec(): Boolean = {
      runActivity()
      true
    }
  }

  override def userContext: UserContext = startedByUser
}