package org.silkframework.runtime.activity

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Waiting}
import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.metrics.MeterRegistryProvider

import java.time.Instant
import java.util.concurrent._
import java.util.logging.Level
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.control.NonFatal

private class ActivityExecution[T](activity: Activity[T],
                                   parent: Option[ActivityContext[_]] = None,
                                   progressContribution: Double = 0.0,
                                   projectAndTaskId: Option[ProjectAndTaskIds])
    extends ActivityMonitor[T](activity.name, parent, progressContribution, activity.initialValue, projectAndTaskId = projectAndTaskId)
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
  private var queueTimestamp: Option[Instant] = None

  @volatile
  private var startTimestamp: Option[Instant] = None

  @volatile
  private var cancelTimestamp: Option[Instant] = None

  // Locks the access to the runningThread variable
  private object ThreadLock

  // Locks the access to the status variable when needed
  private object StatusLock

  // Set to request one additional run. Only ever accessed under StatusLock.
  private var reRunRequested: Boolean = false

  @volatile
  private var runningThread: Option[Thread] = None

  override def queueTime: Option[Instant] = queueTimestamp

  override def startTime: Option[Instant] = startTimestamp

  override def startedBy: UserContext = startedByUser

  override def start()(implicit user: UserContext): Unit = {
    initStatus()
    runForkJoin()
  }

  // Checks if the activity is already running (and fails if it is) and inits the status.
  private def initStatus(allowWaiting: Boolean = false)(implicit user: UserContext): Unit = {
    StatusLock.synchronized {
      if (status().isRunning && (!allowWaiting || !status().isInstanceOf[Waiting])) {
        throw new IllegalStateException(s"Cannot start while activity '${this.activity.name}' is still running!")
      }
      reRunRequested = false // Discard stale re-run requests of previous executions
      setStartMetaData(user)
    }
    activity.resetCancelFlag()
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    initStatus()
    runForkJoin()
    waitUntilFinished()
  }

  private def setStartMetaData(user: UserContext): Unit = {
    resetMetaData()
    status.update(Status.Waiting())
    this.startedByUser = user
    this.queueTimestamp = Some(Instant.now())
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    initStatus()
    for (v <- initialValue)
      value.update(v)
    runForkJoin()
    waitUntilFinished()
    value()
  }

  override def startPrioritized()(implicit user: UserContext): Unit = {
    initStatus(allowWaiting = true)
    user.user match {
      case Some(u) if u.uri.nonEmpty => log.info(s"Activity '${activity.name}' ${projectAndTaskId.map(_.toString).getOrElse("")} has been " +
        s"started prioritized, skipping the waiting queue. (triggered by user with URI: ${u.uri})")
      case _ => log.info(s"Activity '${activity.name}' ${projectAndTaskId.map(_.toString)} has been started with priority, skipping the waiting queue.")
    }
    for(runner <- forkJoinRunner) {
      // Activity has already been scheduled
      if (!runner.cancel(false)) {
        throw new IllegalStateException(s"Cannot prioritize activity '${this.activity.name}' because the current execution could not be cancelled.")
      }
    }
    runForkJoin(prioritized = true)
  }

  private def runForkJoin(prioritized: Boolean = false)(implicit user: UserContext): Unit = {
    val forkJoin = new ForkJoinRunner()
    StatusLock.synchronized {
      forkJoinRunner = Some(forkJoin)
    }
    if (parent.isDefined && runningInOwnPool(prioritized)) {
      forkJoin.fork()
    } else if(prioritized) {
      ActivityExecution.priorityThreadPool.execute(forkJoin)
    } else {
      ActivityExecution.forkJoinPool.execute(forkJoin)
    }
  }

  override def cancel()(implicit user: UserContext): Unit = {
    var cancelling = false
    StatusLock.synchronized {
      reRunRequested = false // Discard re-runs requested before this cancellation; later requests are honored again
      if (status().isRunning && !status().isInstanceOf[Status.Canceling]) {
        cancelling = true
        this.cancelledByUser = user
        this.cancelTimestamp = Some(Instant.now)
        status.update(Status.Canceling(status().progress))
      }
    }
    if(cancelling) {
      // cancel children outside of lock to not run into dead locks
      children().foreach(_.cancel())
      activity.cancelExecution()
      interruptEnabled.synchronized {
        if (interruptEnabled.get()) {
          runningThread foreach { thread =>
            thread.interrupt() // To interrupt an activity that might be blocking on something else, e.g. slow network connection
          }
        }
      }
    }
  }

  override def reset()(implicit userContext: UserContext): Unit = {
    activity.initialValue.foreach(value.update)
    activity.reset()
    activity.resetCancelFlag()
  }

  override def startOrReRun()(implicit user: UserContext): Unit = {
    val doStart = StatusLock.synchronized {
      val currentStatus = status()
      if (currentStatus.isRunning) {
        // A still-queued run (Waiting) already covers this call; only an in-progress run needs a re-run.
        if (!currentStatus.isInstanceOf[Waiting]) {
          reRunRequested = true
        }
        false
      } else {
        reRunRequested = false
        setStartMetaData(user)
        true
      }
    }
    if (doStart) {
      activity.resetCancelFlag()
      runForkJoin()
    }
  }

  /** Restarts the activity. */
  override def restart()(implicit userContext: UserContext): Future[Unit] = {
    import ActivityExecution.activityManagementExecutionContext
    cancel()
    Future {
      Try(waitUntilFinished()) // Ignore if the previous execution failed
      try {
        start()
      } catch {
        case _: IllegalStateException => // ignore possible race condition that the activity was started since the check
      }
    }
  }

  def waitUntilFinished(): Unit = {
    for (runner <- forkJoinRunner) {
      try {
        runner.join()
      } catch {
        case NonFatal(ex) =>
          status() match {
            case Finished(false, _, _, Some(cause)) =>
              if(!activity.wasCancelled()) {
                throw cause
              }
            case _ =>
              throw ex
          }
      }
    }
    for (ex <- status().exception if !activity.wasCancelled()) {
      throw ex
    }
  }

  override def underlying: Activity[T] = activity

  private def runActivity(runner: ForkJoinRunner)(implicit user: UserContext): Unit = synchronized {
    markRunning()
    try {
      runWithReRuns()
    } catch {
      // Backstop for unexpected errors outside the per-run handling, e.g., from status observers.
      // Failures of the activity itself have already updated the status and are rethrown unchanged.
      case ex: Throwable if !status().isInstanceOf[Finished] =>
        finishWithFailure(ex)
    } finally {
      cleanupAfterRun(runner)
    }
  }

  private def markRunning(): Unit = {
    status.update(Status.Running("Running", None))
    ThreadLock.synchronized {
      runningThread = Some(Thread.currentThread())
    }
  }

  /* Runs the activity, re-running it as long as startOrReRun() requested another run during the previous one.
     Re-running on the same thread keeps a single fork join runner, so waitUntilFinished() covers all runs. */
  private def runWithReRuns()(implicit user: UserContext): Unit = {
    var runAgain = true
    while (runAgain) {
      startTimestamp = Some(Instant.now)
      try {
        // Skip the body if cancelled before this run started, but still finish via finishOrRequestReRun().
        if (!activity.wasCancelled() && !parent.exists(_.status().isInstanceOf[Canceling])) {
          activity.run(this)
        }
        runAgain = finishOrRequestReRun()
      } catch {
        case ex: Throwable =>
          runAgain = reRunAfterFailure(ex)
      }
    }
  }

  /* Atomically with the finish transition, decides whether to run again because startOrReRun() was called during the
     run. Under StatusLock, a concurrent startOrReRun() either sets the flag before this point or observes the
     finished status and starts fresh. Since cancel() clears the flag, a set flag always postdates the last
     cancellation and is honored even if the current run has been cancelled. */
  private def finishOrRequestReRun()(implicit user: UserContext): Boolean = StatusLock.synchronized {
    if (reRunRequested) {
      prepareReRun()
      true
    } else {
      status.update(Status.Finished(success = true, elapsedSinceStart, cancelled = activity.wasCancelled()))
      false
    }
  }

  /* Same as finishOrRequestReRun(), but for a failed run, e.g., a cancelled run that exited via thread interruption.
     Fatal errors always finish the activity. InterruptedException is the normal exit of a cancelled run, so it must
     not count as fatal here even though NonFatal rejects it. */
  private def reRunAfterFailure(ex: Throwable)(implicit user: UserContext): Boolean = StatusLock.synchronized {
    if (reRunRequested && (NonFatal(ex) || ex.isInstanceOf[InterruptedException])) {
      log.log(Level.WARNING, s"Activity '${activity.name}' failed. Executing the requested re-run regardless.", ex)
      prepareReRun()
      true
    } else {
      finishWithFailure(ex)
      false
    }
  }

  /* Prepares the next run while still holding the StatusLock: once the status is back to Running, a concurrent
     cancel() applies to the new run. A cancellation of the previous run must not carry over. */
  private def prepareReRun()(implicit user: UserContext): Unit = {
    reRunRequested = false
    Thread.interrupted() // Discard a pending interrupt from a cancelled previous run
    activity.resetCancelFlag()
    status.update(Status.Running("Running", None))
  }

  private def finishWithFailure(ex: Throwable): Unit = StatusLock.synchronized {
    reRunRequested = false // Discard a pending re-run request, only relevant for fatal errors
    status.update(Status.Finished(success = false, elapsedSinceStart, cancelled = activity.wasCancelled(), Some(ex)))
    if (!activity.wasCancelled()) {
      throw ex
    }
  }

  private def elapsedSinceStart: Long = {
    System.currentTimeMillis - startTimestamp.map(_.toEpochMilli).getOrElse(System.currentTimeMillis)
  }

  private def cleanupAfterRun(runner: ForkJoinRunner): Unit = {
    if (children().nonEmpty) {
      log.warning(s"Child activities are still being held after completion of parent activity: ${children().map(_.underlying.name).mkString(", " )}")
      clearChildren()
    }
    lastResult = activityExecutionResult
    StatusLock.synchronized {
      // Do not clear a runner that a concurrent start has registered in the meantime
      if (forkJoinRunner.contains(runner)) {
        forkJoinRunner = None
      }
    }
    ThreadLock.synchronized {
      runningThread = None
    }
    // Clear interrupt flag
    Thread.interrupted()
  }


  private def activityExecutionResult: ActivityExecutionResult[T] = {
    ActivityExecutionResult(
      metaData = ActivityExecutionMetaData(
        startedByUser = startedByUser.user,
        queuedAt = queueTimestamp,
        startedAt = startTimestamp,
        finishedAt = Some(Instant.now),
        cancelledAt = cancelTimestamp,
        cancelledBy = cancelledByUser.user,
        finishStatus = status.get
      ),
      resultValue = value.get
    )
  }

  private def resetMetaData(): Unit = {
    // Reset values
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
      runActivity(this)
      true
    }
  }
}

object ActivityExecution {
  // The number of threads that always exist
  private final val CORE_POOL_SIZE = 2
  // The max. number of threads in the pool
  private final val MAX_POOL_SIZE = 32
  // How long are extra threads kept alive, 1 second
  private final val KEEP_ALIVE_MS = 1000L
  // Thread pool used for managing activities asynchronously, e.g. restart.
  implicit val activityManagementExecutionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Execution.createFixedThreadPool(
    "activity-management-thread",
    CORE_POOL_SIZE,
    maxPoolSize = Some(MAX_POOL_SIZE),
    keepAliveInMs = KEEP_ALIVE_MS
  ))

  /**
    * The size of the fork join thread pool
    */
  private def poolSize: Option[Int] = {
    val poolSizePath = "org.silkframework.runtime.activity.poolSize"
    val config = DefaultConfig.instance()
    if (config.hasPath(poolSizePath)) {
      Option(config.getInt(poolSizePath))
    } else {
      None
    }
  }

  /**
    * The fork join pool used to run activities.
    */
  val forkJoinPool: ForkJoinPool = registerMetrics(
    executor = Execution.createForkJoinPool("Activity", size = poolSize),
    name = "Activity",
    tags = List(Tag.of("activity", "normal"))
  )

  /**
    * Thread pool to execute prioritized threads.
    */
  val priorityThreadPool: ForkJoinPool = registerMetrics(
    executor = Execution.createForkJoinPool("Activity-Prio", size = poolSize),
    name = "Activity-Prio",
    tags = List(Tag.of("activity", "priority"))
  )

  /**
   * Registers Micrometer-based JVM metrics for a given ExecutorService.
   *
   * @param executor Executor to monitor.
   * @param name Name of the Executor within the metrics system.
   * @param tags Tags for the metrics system.
   * @tparam E Type parameter for the specific ExecutorService subtype.
   * @return
   */
  private def registerMetrics[E <: ExecutorService](executor: E, name: String, tags: List[Tag]): E = {
    val metrics = new ExecutorServiceMetrics(executor, name, tags.asJava)
    metrics.bindTo(MeterRegistryProvider.meterRegistry)
    executor
  }
}