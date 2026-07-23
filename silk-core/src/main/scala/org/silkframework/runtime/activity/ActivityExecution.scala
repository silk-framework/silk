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

  // Locks the access to the status variable when needed.
  // Eager field, not an inner object: lazy object init locks the instance and deadlocks against runActivity().
  private val StatusLock = new Object

  // Requests one additional run on behalf of the given user. Only ever accessed under StatusLock.
  private var pendingReRun: Option[UserContext] = None

  // Only written by the worker thread (serialized via runActivity's monitor), read by cancel().
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
      prepareFreshStart(user)
    }
  }

  // Must be called under StatusLock.
  private def prepareFreshStart(user: UserContext): Unit = {
    pendingReRun = None // Discard stale re-run requests of previous executions
    setStartMetaData(user)
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
    val trigger = user.user.filter(_.uri.nonEmpty).map(u => s" (triggered by user with URI: ${u.uri})").getOrElse("")
    log.info(s"Activity '${activity.name}' ${projectAndTaskId.map(_.toString).getOrElse("")} has been started prioritized, " +
      s"skipping the waiting queue.$trigger")
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
      pendingReRun = None // Discard re-runs requested before this cancellation; later requests are honored again
      if (status().isRunning && !status().isInstanceOf[Canceling]) {
        cancelling = true
        this.cancelledByUser = user
        this.cancelTimestamp = Some(Instant.now)
        status.update(Status.Canceling(status().progress))
        CancelDeliveries.begin() // Held until the side effects below are delivered; see drainCancellationsAndResetCancelState().
      }
    }
    if(cancelling) {
      // Wake up a draining run: being cancelled itself, it does not need to keep waiting
      CancelDeliveries.notifyWaiters()
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
        CancelDeliveries.end()
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
          pendingReRun = Some(user) // The re-run executes on behalf of this caller, not the original starter
        }
        false
      } else {
        prepareFreshStart(user)
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

  /* Synchronized on this instance: a fresh start may register a new runner while the previous runner is still in its
     cleanup, and the two executions must not overlap. */
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
    runningThread = Some(Thread.currentThread())
  }

  /* Runs the activity, re-running it as long as startOrReRun() requested another run during the previous one.
     Re-running on the same thread keeps a single fork join runner, so waitUntilFinished() covers all runs.
     Each re-run executes on behalf of the user who requested it. */
  private def runWithReRuns(runner: ForkJoinRunner, initialUser: UserContext): Unit = {
    var currentUser: Option[UserContext] = Some(initialUser)
    var isInitialRun = true
    while (currentUser.isDefined) {
      implicit val user: UserContext = currentUser.get
      currentUser = try {
        drainCancellationsAndResetCancelState()
        startTimestamp = Some(Instant.now)
        // Skip the body if cancelled before this run started (even if the cancellation's side effects are still
        // being delivered and the cancel flag is not set yet), but still finish via finishOrRequestReRun().
        if (cancelTimestamp.isEmpty && !activity.wasCancelled() && !parent.exists(_.status().isInstanceOf[Canceling])) {
          activity.run(this)
        }
        finishOrRequestReRun()
      } catch {
        case ex: Throwable =>
          reRunAfterFailure(ex, runner, isInitialRun)
      }
      isInitialRun = false
    }
  }

  /* Atomically with the finish transition, decides whether to run again because startOrReRun() was called during the
     run, returning the user context for the next run if so. Under StatusLock, a concurrent startOrReRun() either
     records its request before this point or observes the finished status and starts fresh. Since cancel() clears the
     request, a pending request always postdates the last cancellation and is honored even if the current run has been
     cancelled. */
  private def finishOrRequestReRun(): Option[UserContext] = StatusLock.synchronized {
    if (pendingReRun.isDefined) {
      Some(prepareReRun())
    } else {
      publishFinished(success = true, cancelled = activity.wasCancelled() || cancelTimestamp.isDefined)
      None
    }
  }

  /* Same as finishOrRequestReRun(), but for a failed run, e.g., a cancelled run that exited via thread interruption.
     Fatal errors always finish the activity. InterruptedException is the normal exit of a cancelled run, so it must
     not count as fatal here even though NonFatal rejects it. Any other failure is genuine, even if it raced with a
     cancellation, and is published as a transient Finished status, so the re-run's final status does not silently
     mask it. */
  private def reRunAfterFailure(ex: Throwable, runner: ForkJoinRunner, isInitialRun: Boolean): Option[UserContext] = StatusLock.synchronized {
    if (pendingReRun.isDefined && (NonFatal(ex) || ex.isInstanceOf[InterruptedException])) {
      val cancelled = cancelTimestamp.isDefined || activity.wasCancelled()
      if (cancelled && ex.isInstanceOf[InterruptedException]) {
        // The normal exit of a cancelled run, not a failure
        log.log(Level.INFO, s"Cancelled activity '${activity.name}' exited via interruption. Executing the requested re-run.", ex)
      } else {
        if (isInitialRun && !cancelled) {
          runner.initialRunFailure = Some(ex) // Blocking callers of the failed initial run must still see its failure
        }
        // Publish the failure before the re-run (this also logs it)
        publishFinished(success = false, cancelled = cancelled, exception = Some(ex))
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
    val nextUser = pendingReRun.getOrElse(startedByUser) // The re-run executes on behalf of the user who requested it
    pendingReRun = None
    startedByUser = nextUser
    resetMetaData() // The re-run is not cancelled: drop the previous run's cancellation metadata
    status.update(Status.Running("Running", None), force = true) // Must win over the Canceling state of a cancelled previous run
    nextUser
  }

  /* Publishes the terminal status of a run and records the execution result, even if a status observer fails during
     the publish. Must be called under StatusLock, so a concurrent restart cannot corrupt the recorded result. */
  private def publishFinished(success: Boolean, cancelled: Boolean, exception: Option[Throwable] = None): Unit = {
    try {
      status.update(Status.Finished(success, elapsedSinceStart, cancelled, exception))
    } finally {
      lastResult = activityExecutionResult
    }
  }

  /* Runs on the worker thread before each run's body, outside StatusLock, so control operations stay responsive while
     a slow cancellation is delivering. Waits out any in-flight cancellation delivery, then clears the leftover cancel
     state of the previous run — unless a cancel() has targeted this scheduled run (cancelTimestamp set), in which case
     there is nothing to clear and the body is skipped or cancelled anyway. */
  private def drainCancellationsAndResetCancelState()(implicit user: UserContext): Unit = {
    CancelDeliveries.awaitNoneUnless(skipWaiting = cancelTimestamp.isDefined)
    StatusLock.synchronized {
      if (cancelTimestamp.isEmpty) { // No cancel() targeted this scheduled run
        Thread.interrupted() // Discard a pending interrupt from a cancelled previous run
        activity.resetCancelFlag()
      }
    }
  }

  private def finishWithFailure(ex: Throwable): Unit = StatusLock.synchronized {
    pendingReRun = None // Discard a pending re-run request, only relevant for fatal errors
    publishFinished(success = false, cancelled = activity.wasCancelled(), exception = Some(ex))
    if (!activity.wasCancelled()) {
      throw ex
    }
  }

  private def elapsedSinceStart: Long = {
    startTimestamp.map(start => System.currentTimeMillis - start.toEpochMilli).getOrElse(0L)
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
    runningThread = None
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

  /* Cancellations still delivering their side effects (cancelExecution(), interrupts) outside StatusLock. Each run
     waits for the count to reach zero before its body starts, so a cancellation cannot leak into a later run — unless
     the cancellation targets that run itself. Control operations never wait on this, so they stay responsive while a
     slow cancellation is delivering.
     Eager field, not an inner object: lazy object init inside cancel() (under StatusLock) locks the instance and
     deadlocks against a worker picking up the queued run (runActivity() holds the instance, then takes StatusLock). */
  private val CancelDeliveries = new CancelDeliveryTracker

  private final class CancelDeliveryTracker {
    private val inFlight = new AtomicInteger(0)

    // Registers a delivery. Called under StatusLock, atomically with the Canceling status transition.
    def begin(): Unit = inFlight.incrementAndGet()

    // Marks a delivery as finished and wakes up runs waiting for it.
    def end(): Unit = synchronized {
      inFlight.decrementAndGet()
      notifyAll()
    }

    // Wakes up waiting runs so they re-evaluate their skip condition.
    def notifyWaiters(): Unit = synchronized {
      notifyAll()
    }

    // Blocks until no delivery is in flight or the skip condition holds. Absorbs interrupts of awaited cancellations.
    def awaitNoneUnless(skipWaiting: => Boolean): Unit = synchronized {
      while (inFlight.get() != 0 && !skipWaiting) {
        try wait() catch { case _: InterruptedException => }
      }
    }
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