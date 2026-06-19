package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Waiting}
import org.silkframework.runtime.activity._
import org.silkframework.workspace.ProjectTask

import java.time.Instant
import java.util.concurrent.{CompletableFuture, CompletionException, Future => JFuture}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

private[activity] class QueuedActivityControl[T](delegate: ActivityControl[T],
                                                 task: Option[ProjectTask[_ <: TaskSpec]],
                                                 factory: WorkspaceActivityFactory,
                                                 limit: ActivityLimit)
    extends ActivityControl[T] {

  private implicit val restartExecutionContext: ExecutionContext = ActivityExecution.activityManagementExecutionContext

  private object Lock

  // The wrapper has to remember the user before the delegate actually starts, because queue waiting happens outside the delegate.
  @volatile
  private var pendingUser: UserContext = UserContext.Empty

  @volatile
  private var queuedAt: Option[Instant] = None

  @volatile
  private var queuedCanceled = false

  // True once this wrapper run has handed control over to the underlying ActivityControl.
  @volatile
  private var delegateStarted = false

  // Marks that the current queued run should skip the limiter and start immediately.
  @volatile
  private var bypassLimiter = false

  // Identifies the current wrapper-managed run and invalidates stale async callbacks from older runs.
  @volatile
  private var currentRunId = 0L

  @volatile
  private var waitFuture: Option[JFuture[_]] = None

  @volatile
  private var currentQueuedToken: Option[ActivityExecutionLimiter.QueueToken] = None

  @volatile
  private var currentLimiterKey: Option[ActivityLimiterKey] = None

  // Tracks wrapper completion, including queued cancellation before the delegate ever starts.
  @volatile
  private var completionFuture: Option[CompletableFuture[Unit]] = None

  private val wrappedStatus = new ValueHolder[Status](Some(delegate.status()))

  private val delegateStatusListener: Status => Unit = { status =>
    if(delegateStarted) {
      wrappedStatus.update(status)
    }
  }

  delegate.status.subscribe(delegateStatusListener)

  override def name: String = delegate.name

  override def value: Observable[T] = delegate.value

  override def status: Observable[Status] = wrappedStatus

  override def queueTime: Option[Instant] = queuedAt.orElse(delegate.queueTime)

  override def startTime: Option[Instant] = delegate.startTime

  override def startedBy: UserContext = {
    if(delegateStarted) delegate.startedBy else pendingUser
  }

  override def children(): Seq[ActivityControl[_]] = delegate.children()

  override def start()(implicit user: UserContext): Unit = {
    startInternal(prioritized = false)
  }

  // Restarts the wrapper-managed run, so queued executions are cancelled and re-enter through the limiter again.
  override def restart()(implicit user: UserContext): Future[Unit] = {
    cancel()
    Future {
      Try(waitUntilFinished())
      try {
        start()
      } catch {
        case _: IllegalStateException =>
      }
    }
  }

  // Blocks on the wrapper completion, which also covers cancellation or failure before the delegate starts.
  override def startBlocking()(implicit user: UserContext): Unit = {
    start()
    waitUntilFinished()
  }

  override def startBlockingAndGetValue(initialValue: Option[T])(implicit user: UserContext): T = {
    for {
      valueHolder <- Option(delegate.value).collect { case holder: ValueHolder[T @unchecked] => holder }
      initial <- initialValue
    } {
      valueHolder.update(initial)
    }
    startBlocking()
    value()
  }

  // If the run is still queued, this skips the limiter and starts the delegate immediately instead of just reprioritizing inside the delegate.
  override def startPrioritized()(implicit user: UserContext): Unit = {
    val (queuedRun, prioritizeDelegate) =
      Lock.synchronized {
        if(isQueued) {
          bypassLimiter = true
          val limiterState = currentLimiterKey.flatMap { key =>
            currentQueuedToken.map(token => (key, token))
          }
          currentQueuedToken = None
          (Some((currentRunId, currentCompletionFuture(), waitFuture, limiterState)), false)
        } else if(delegateStarted) {
          (None, true)
        } else {
          (None, false)
        }
      }

    queuedRun match {
      case Some((queuedRunId, currentCompletion, queuedWaitFuture, limiterState)) =>
        limiterState.foreach { case (key, token) =>
          ActivityExecutionLimiter.cancelQueued(key, token)
        }
        queuedWaitFuture.foreach(_.cancel(true))
        startDelegate(queuedRunId, prioritized = true, permit = None, currentCompletion)
      case None if prioritizeDelegate =>
        delegate.startPrioritized()
      case None =>
        startInternal(prioritized = true)
    }
  }

  // Cancels either the queued wrapper run or the already started delegate execution, depending on the current state.
  override def cancel()(implicit user: UserContext): Unit = {
    val (futureToCancel, limiterState) =
      Lock.synchronized {
        if(isQueued) {
          queuedCanceled = true
          wrappedStatus.update(Canceling(wrappedStatus().progress))
          val limiterState = currentLimiterKey.flatMap { key =>
            currentQueuedToken.map(token => (key, token))
          }
          currentQueuedToken = None
          (waitFuture, limiterState)
        } else {
          (None, None)
        }
      }
    limiterState.foreach { case (key, token) =>
      ActivityExecutionLimiter.cancelQueued(key, token)
    }
    futureToCancel match {
      case Some(future) =>
        future.cancel(true)
      case None =>
        delegate.cancel()
    }
  }

  override def reset()(implicit userContext: UserContext): Unit = {
    delegate.reset()
  }

  override def underlying: Activity[T] = delegate.underlying

  override def waitUntilFinished(): Unit = {
    try {
      currentCompletionFuture().join()
    } catch {
      case ex: CompletionException =>
        throw Option(ex.getCause).getOrElse(ex)
    }
  }

  override def lastResult: Option[ActivityExecutionResult[T]] = delegate.lastResult

  // Starts the wrapper-managed run: either by queuing for a permit or by delegating immediately when no limit applies.
  private def startInternal(prioritized: Boolean)(implicit user: UserContext): Unit = {
    val (runId, currentLimit, currentKey, completion) = Lock.synchronized {
      if(wrappedStatus().isRunning) {
        throw new IllegalStateException(s"Cannot start while activity '$name' is still running!")
      }
      currentRunId += 1
      pendingUser = user
      queuedAt = Some(Instant.now())
      queuedCanceled = false
      delegateStarted = false
      bypassLimiter = prioritized
      currentQueuedToken = None
      val completionFuture = new CompletableFuture[Unit]()
      this.completionFuture = Some(completionFuture)
      val limitValue = if(prioritized) {
        None
      } else {
        limit.limitFor(task, factory)
      }
      val limiterKey = limit.limiterKey(task.map(_.project.id), task.map(_.id))
      currentLimiterKey = Some(limiterKey)
      (currentRunId, limitValue, limiterKey, completionFuture)
    }

    if(prioritized || currentLimit.isEmpty) {
      startDelegate(runId, prioritized = prioritized, permit = None, completion)
    } else {
      val waitingText = limit.waitingMessage(task)
      Lock.synchronized {
        wrappedStatus.update(Waiting(waitingText))
      }
      val submittedFuture = ActivityExecutionLimiter.runAsync {
        try {
          val permit = ActivityExecutionLimiter.awaitPermit(
            key = currentKey,
            currentLimit = limit.limitFor(task, factory),
            isCancelled = () => queuedCanceled || bypassLimiter || currentRunId != runId,
            onQueuedTokenChanged = token =>
              Lock.synchronized {
                if(currentRunId == runId && !delegateStarted) {
                  currentQueuedToken = token
                }
              }
          )
          permit match {
            case Some(acquiredPermit) =>
              startDelegate(runId, prioritized = false, permit = Some(acquiredPermit), completion)
            case None =>
              if(isCurrentQueuedRun(runId)) {
                finishQueuedCancellation(runId, completion)
              }
            }
        } catch {
          case _: InterruptedException =>
            if(isCurrentQueuedRun(runId)) {
              finishQueuedCancellation(runId, completion)
            }
            Thread.currentThread().interrupt()
          case NonFatal(ex) =>
            if(isCurrentRun(runId)) {
              completeFailure(runId, ex, completion)
            }
        }
      }
      Lock.synchronized {
        if(currentRunId == runId && !delegateStarted && wrappedStatus().isRunning) {
          waitFuture = Some(submittedFuture)
        }
      }
    }
  }

  // Hands execution over to the underlying control and completes the wrapper future when the delegate finishes.
  private def startDelegate(runId: Long,
                            prioritized: Boolean,
                            permit: Option[ActivityExecutionLimiter.ActivityExecutionPermit],
                            completion: CompletableFuture[Unit]): Unit = {
    QueuedActivityControlTestHooks.beforeDelegateStart(permit.isDefined)

    val (shouldStart, cancelledBeforeDelegateStart) =
      Lock.synchronized {
        if(currentRunId != runId || delegateStarted) {
          (false, false)
        } else if(queuedCanceled) {
          (false, true)
        } else {
          delegateStarted = true
          queuedCanceled = false
          bypassLimiter = false
          waitFuture = None
          (true, false)
        }
      }
    if(cancelledBeforeDelegateStart) {
      permit.foreach(_.release())
      finishQueuedCancellation(runId, completion)
      return
    }
    if(!shouldStart) {
      permit.foreach(_.release())
      return
    }

    try {
      implicit val user: UserContext = pendingUser
      if(prioritized) {
        delegate.startPrioritized()
      } else {
        delegate.start()
      }
      ActivityExecutionLimiter.runAsync {
        try {
          delegate.waitUntilFinished()
          completion.complete(())
        } catch {
          case NonFatal(ex) =>
            completion.completeExceptionally(ex)
        } finally {
          permit.foreach(_.release())
          clearRunState(runId)
        }
      }
    } catch {
      case NonFatal(ex) =>
        permit.foreach(_.release())
        completeFailure(runId, ex, completion)
    }
  }

  // Completes a run that never reached the delegate because it was cancelled while still waiting in the limiter queue.
  private def finishQueuedCancellation(runId: Long, completion: CompletableFuture[Unit]): Unit = {
    val cancellationStatus =
      Lock.synchronized {
        if(currentRunId != runId || delegateStarted) {
          None
        } else {
          val runtime = queuedAt.map(startedAt => java.time.Duration.between(startedAt, Instant.now()).toMillis).getOrElse(0L)
          val status = Finished(success = false, runtime = runtime, cancelled = true)
          wrappedStatus.update(status)
          Some(status)
        }
      }
    cancellationStatus.foreach { _ =>
      completion.complete(())
      clearRunState(runId)
    }
  }

  // Fails a run before the delegate could take over, e.g. due to queue waiting or delegate startup errors.
  private def completeFailure(runId: Long, ex: Throwable, completion: CompletableFuture[Unit]): Unit = {
    Lock.synchronized {
      if(currentRunId == runId && !delegateStarted) {
        val runtime = queuedAt.map(startedAt => java.time.Duration.between(startedAt, Instant.now()).toMillis).getOrElse(0L)
        wrappedStatus.update(Finished(success = false, runtime = runtime, cancelled = false, exception = Some(ex)))
      }
    }
    completion.completeExceptionally(ex)
    clearRunState(runId)
  }

  // Clears only wrapper-owned transient state. Delegate-owned execution/result state stays on the wrapped control.
  private def clearRunState(runId: Long): Unit = {
    Lock.synchronized {
      if(currentRunId == runId) {
        queuedCanceled = false
        waitFuture = None
        delegateStarted = false
        bypassLimiter = false
        currentQueuedToken = None
        currentLimiterKey = None
      }
    }
  }

  private def currentCompletionFuture(): CompletableFuture[Unit] = {
    completionFuture.getOrElse {
      val completedFuture = new CompletableFuture[Unit]()
      completedFuture.complete(())
      completedFuture
    }
  }

  private def isQueued: Boolean = {
    wrappedStatus().isInstanceOf[Waiting] && !delegateStarted
  }

  private def isCurrentQueuedRun(runId: Long): Boolean = Lock.synchronized {
    currentRunId == runId && !delegateStarted
  }

  private def isCurrentRun(runId: Long): Boolean = Lock.synchronized {
    currentRunId == runId
  }

}

private[activity] object QueuedActivityControlTestHooks {
  @volatile
  private var beforeDelegateStartHook: Boolean => Unit = _ => ()

  def beforeDelegateStart(permitAcquired: Boolean): Unit = {
    beforeDelegateStartHook(permitAcquired)
  }

  def setBeforeDelegateStartHook(hook: Boolean => Unit): Unit = {
    beforeDelegateStartHook = hook
  }

  def reset(): Unit = {
    beforeDelegateStartHook = _ => ()
  }
}
