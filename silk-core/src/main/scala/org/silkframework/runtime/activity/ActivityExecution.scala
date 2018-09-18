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

  // Locks the access to the runningThread variable
  private object ThreadLock

  @volatile
  private var runningThread: Option[Thread] = None

  override def startTime: Option[Long] = startTimestamp

  override def start()(implicit user: UserContext): Unit = {
    // Check if the current activity is still running
    if (status().isRunning) {
      throw new IllegalStateException(s"Cannot start while activity ${this.activity.name} is still running!")
    }
    // FIXME: Here is a mini race condition if two threads call start() at the same time, see CMEM-934
    setStartMetaData(user)
    // Execute activity
    val forkJoin = new ForkJoinRunner()
    forkJoinRunner = Some(forkJoin)
    if (parent.isDefined) {
      forkJoin.fork()
    } else {
      Activity.forkJoinPool.execute(forkJoin)
    }
  }

  override def startBlocking()(implicit user: UserContext): Unit = synchronized {
    setStartMetaData(user)
    runActivity()
  }

  private def setStartMetaData(user: UserContext) = {
    resetMetaData()
    status.update(Status.Started())
    this.startedByUser = user
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = synchronized {
    setStartMetaData(user)
    for (v <- initialValue)
      value.update(v)
    runActivity()
    value()
  }

  override def cancel()(implicit user: UserContext): Unit = {
    if (status().isRunning && !status().isInstanceOf[Status.Canceling]) {
      this.cancelledByUser = user
      this.cancelTimestamp = Some(System.currentTimeMillis())
      status.update(Status.Canceling(status().progress))
      children().foreach(_.cancel())
      activity.cancelExecution()
      ThreadLock.synchronized {
        runningThread foreach { thread =>
          thread.interrupt() // To interrupt an activity that might be blocking on something else, e.g. slow network connection
        }
      }
    }
  }

  override def reset()(implicit userContext: UserContext): Unit = {
    activity.initialValue.foreach(value.update)
    activity.reset()
    activity.resetCancelFlag()
  }

  def waitUntilFinished(): Unit = {
    for (runner <- forkJoinRunner) {
      try {
        runner.join()
      } catch {
        case NonFatal(ex) =>
          status() match {
            case Finished(false, _, _, Some(cause)) =>
              throw cause
            case _ =>
              throw ex
          }
      }
    }
  }

  override def underlying: Activity[T] = activity

  private def runActivity()(implicit user: UserContext): Unit = synchronized {
    ThreadLock.synchronized {
      runningThread = Some(Thread.currentThread())
    }
    activity.resetCancelFlag()
    if (!parent.exists(_.status().isInstanceOf[Canceling])) {
      val startTime = System.currentTimeMillis()
      startTimestamp = Some(startTime)
      try {
        activity.run(this)
        status.update(Status.Finished(success = true, System.currentTimeMillis - startTime, cancelled = activity.wasCancelled()))
      } catch {
        case ex: Throwable =>
          status.update(Status.Finished(success = false, System.currentTimeMillis - startTime, cancelled = activity.wasCancelled(), Some(ex)))
          throw ex
      } finally {
        lastResult = activityExecutionResult
        resetMetaData()
        forkJoinRunner = None
        ThreadLock.synchronized {
          runningThread = None
        }
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
}