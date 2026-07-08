package org.silkframework.runtime.activity

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Waiting}
import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.metrics.MeterRegistryProvider

import java.time.Instant
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
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

  // Cancellations still delivering their side effects outside StatusLock. Each run waits on this object's monitor
  // for the counter to reach zero before its body starts, so a cancellation cannot leak into a later run.
  private val pendingCancellations = new AtomicInteger(0)

  // Set to request one additional run, and the user who requested it. Only ever accessed under StatusLock.
  private var reRunRequested: Boolean = false
  private var reRunUser: Option[UserContext] = None

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
      clearReRunRequest() // Discard stale re-run requests of previous executions
      setStartMetaData(user)
    }
  }

  override def startBlocking()(implicit user: UserContext): Unit = {
    initStatus()
    val runner = runForkJoin()
    waitUntilFinished(Some(runner))
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
    val runner = runForkJoin()
    waitUntilFinished(Some(runner))
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

  private def runForkJoin(prioritized: Boolean = false)(implicit user: UserContext): ForkJoinRunner = {
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
    forkJoin
  }

  override def cancel()(implicit user: UserContext): Unit = {
    var cancelling = false
    StatusLock.synchronized {
      clearReRunRequest() // Discard re-runs requested before this cancellation; later requests are honored again
      if (status().isRunning && !status().isInstanceOf[Status.Canceling]) {
        cancelling = true
        this.cancelledByUser = user
        this.cancelTimestamp = Some(Instant.now)
        status.update(Status.Canceling(status().progress))
        pendingCancellations.incrementAndGet() // Held until the side effects below are delivered; see drainCancellationsAndResetCancelState().
      }
    }
    if(cancelling) {
      // Wake up a draining run: being cancelled itself, it does not need to keep waiting
      pendingCancellations.synchronized {
        pendingCancellations.notifyAll()
      }
      try {
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
      } finally {
        pendingCancellations.synchronized {
          pendingCancellations.decrementAndGet()
          pendingCancellations.notifyAll() // Wake up a run waiting for this delivery
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
          reRunUser = Some(user) // The re-run executes on behalf of this caller, not the original starter
        }
        false
      } else {
        clearReRunRequest()
        setStartMetaData(user)
        true
      }
    }
    if (doStart) {
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

  def waitUntilFinished(): Unit = waitUntilFinished(forkJoinRunner)

  private def waitUntilFinished(runnerOpt: Option[ForkJoinRunner]): Unit = {
    for (runner <- runnerOpt) {
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
      // A genuine failure of the initial run must reach its blocking starter, even if a coalesced re-run succeeded
      for (ex <- runner.initialRunFailure) {
        throw ex
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
      runWithReRuns(runner, user)
    } catch {
      // Backstop for unexpected errors outside the per-run handling, e.g., from status observers.
      // Failures of the activity itself have already updated the status and are rethrown unchanged.
      case ex: Throwable if !status().isInstanceOf[Finished] =>
        finishWithFailure(ex)
    } finally {
      cleanupAfterRun(runner)
    }
  }

  /* The status transition happens under StatusLock, so it cannot interleave with a concurrent cancel(): either the
     cancel came first and the Canceling status rejects this Running update, or cancel() observes the Running status
     and targets this run normally. */
  private def markRunning(): Unit = {
    StatusLock.synchronized {
      status.update(Status.Running("Running", None))
    }
    ThreadLock.synchronized {
      runningThread = Some(Thread.currentThread())
    }
  }

  /* Runs the activity, re-running it as long as startOrReRun() requested another run during the previous one.
     Re-running on the same thread keeps a single fork join runner, so waitUntilFinished() covers all runs.
     Each re-run executes on behalf of the user who requested it. */
  private def runWithReRuns(runner: ForkJoinRunner, initialUser: UserContext): Unit = {
    var currentUser = initialUser
    var isInitialRun = true
    var runAgain = true
    while (runAgain) {
      implicit val user: UserContext = currentUser
      try {
        drainCancellationsAndResetCancelState()
        startTimestamp = Some(Instant.now)
        // Skip the body if cancelled before this run started (even if the cancellation's side effects are still
        // being delivered and the cancel flag is not set yet), but still finish via finishOrRequestReRun().
        if (cancelTimestamp.isEmpty && !activity.wasCancelled() && !parent.exists(_.status().isInstanceOf[Canceling])) {
          activity.run(this)
        }
        finishOrRequestReRun() match {
          case Some(nextUser) => currentUser = nextUser
          case None => runAgain = false
        }
      } catch {
        case ex: Throwable =>
          reRunAfterFailure(ex, runner, isInitialRun) match {
            case Some(nextUser) => currentUser = nextUser
            case None => runAgain = false
          }
      }
      isInitialRun = false
    }
  }

  /* Atomically with the finish transition, decides whether to run again because startOrReRun() was called during the
     run, returning the user context for the next run if so. Under StatusLock, a concurrent startOrReRun() either sets
     the flag before this point or observes the finished status and starts fresh. Since cancel() clears the flag, a
     set flag always postdates the last cancellation and is honored even if the current run has been cancelled. */
  private def finishOrRequestReRun(): Option[UserContext] = StatusLock.synchronized {
    if (reRunRequested) {
      Some(prepareReRun())
    } else {
      try {
        status.update(Status.Finished(success = true, elapsedSinceStart, cancelled = activity.wasCancelled() || cancelTimestamp.isDefined))
      } finally {
        lastResult = activityExecutionResult // Snapshot under the lock so a concurrent restart cannot corrupt it; set even if a status observer fails
      }
      None
    }
  }

  /* Same as finishOrRequestReRun(), but for a failed run, e.g., a cancelled run that exited via thread interruption.
     Fatal errors always finish the activity. InterruptedException is the normal exit of a cancelled run, so it must
     not count as fatal here even though NonFatal rejects it. Any other failure is genuine, even if it raced with a
     cancellation, and is published as a transient Finished status, so the re-run's final status does not silently
     mask it. */
  private def reRunAfterFailure(ex: Throwable, runner: ForkJoinRunner, isInitialRun: Boolean): Option[UserContext] = StatusLock.synchronized {
    if (reRunRequested && (NonFatal(ex) || ex.isInstanceOf[InterruptedException])) {
      val cancelled = cancelTimestamp.isDefined || activity.wasCancelled()
      if (cancelled && ex.isInstanceOf[InterruptedException]) {
        // The normal exit of a cancelled run, not a failure
        log.log(Level.INFO, s"Cancelled activity '${activity.name}' exited via interruption. Executing the requested re-run.", ex)
      } else {
        if (isInitialRun && !cancelled) {
          runner.initialRunFailure = Some(ex) // Blocking callers of the failed initial run must still see its failure
        }
        // Publish the failure before the re-run (this also logs it)
        try {
          status.update(Status.Finished(success = false, elapsedSinceStart, cancelled = cancelled, Some(ex)))
        } finally {
          lastResult = activityExecutionResult
        }
      }
      Some(prepareReRun())
    } else {
      finishWithFailure(ex)
      None
    }
  }

  /* Prepares the next run while still holding the StatusLock: once the status is back to Running, a concurrent
     cancel() applies to the new run. Leftover cancel state of the previous run is cleared by
     drainCancellationsAndResetCancelState() before the re-run's body executes. Returns the user on whose behalf
     the re-run executes. */
  private def prepareReRun(): UserContext = {
    val nextUser = reRunUser.getOrElse(startedByUser) // The re-run executes on behalf of the user who requested it
    clearReRunRequest()
    startedByUser = nextUser
    resetMetaData() // The re-run is not cancelled: drop the previous run's cancellation metadata
    status.update(Status.Running("Running", None), force = true) // Must win over the Canceling state of a cancelled previous run
    nextUser
  }

  // Must only be called under StatusLock
  private def clearReRunRequest(): Unit = {
    reRunRequested = false
    reRunUser = None
  }

  /* Runs on the worker thread before each run's body, outside StatusLock, so control operations stay responsive while
     a slow cancellation is delivering. Waits out any in-flight cancellation delivery, then clears the leftover cancel
     state of the previous run — unless a cancel() has targeted this scheduled run (cancelTimestamp set), in which case
     there is nothing to clear and the body is skipped or cancelled anyway. */
  private def drainCancellationsAndResetCancelState()(implicit user: UserContext): Unit = {
    pendingCancellations.synchronized {
      while (pendingCancellations.get() != 0 && cancelTimestamp.isEmpty) {
        try pendingCancellations.wait() catch { case _: InterruptedException => } // Absorb the interrupt of the cancellation we wait for
      }
    }
    StatusLock.synchronized {
      if (cancelTimestamp.isEmpty) { // No cancel() targeted this scheduled run
        Thread.interrupted() // Discard a pending interrupt from a cancelled previous run
        activity.resetCancelFlag()
      }
    }
  }

  private def finishWithFailure(ex: Throwable): Unit = StatusLock.synchronized {
    clearReRunRequest() // Discard a pending re-run request, only relevant for fatal errors
    try {
      status.update(Status.Finished(success = false, elapsedSinceStart, cancelled = activity.wasCancelled(), Some(ex)))
    } finally {
      lastResult = activityExecutionResult // Snapshot under the lock so a concurrent restart cannot corrupt it; set even if a status observer fails
    }
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

    // A genuine failure of this runner's initial run, preserved for blocking callers even if a re-run succeeds after it
    @volatile
    var initialRunFailure: Option[Throwable] = None

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