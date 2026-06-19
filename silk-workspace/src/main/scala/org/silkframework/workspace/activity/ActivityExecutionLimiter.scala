package org.silkframework.workspace.activity

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable

/**
 * Coordinates per-key activity execution limits with FIFO queueing.
 *
 * Waiting for a slot is offloaded to virtual threads so queued workflow starts do not occupy the main activity pools.
 * The limiter keeps only in-memory state and cleans up each key as soon as there are no running executions and no queued waiters left.
 */
object ActivityExecutionLimiter {

  final case class QueueToken private(id: Long)

  sealed trait SlotRequestResult

  final case class Acquired(permit: ActivityExecutionPermit) extends SlotRequestResult

  final case class Queued(token: QueueToken) extends SlotRequestResult

  final class ActivityExecutionPermit private[activity](private val key: ActivityLimiterKey) {
    private val released = new AtomicBoolean(false)

    def release(): Unit = {
      if(released.compareAndSet(false, true)) {
        ActivityExecutionLimiter.release(key)
      }
    }
  }

  private case class ActivityExecutionState(var runningExecutions: Int = 0,
                                            queue: mutable.Queue[QueueToken] = mutable.Queue.empty,
                                            monitor: AnyRef = new Object())

  private val activityStates = mutable.Map.empty[ActivityLimiterKey, ActivityExecutionState]
  private var nextQueueTokenId = 0L

  private val virtualThreadFactory = Thread.ofVirtual().name("activity-limit-", 0L).factory()

  private val virtualThreadExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)

  def runAsync(task: => Unit): java.util.concurrent.Future[_] = {
    virtualThreadExecutor.submit(new Runnable {
      override def run(): Unit = task
    })
  }

  // Either acquires a running slot immediately or appends the caller to the tail of the FIFO queue for this limiter key.
  def requestSlot(key: ActivityLimiterKey, maxParallelExecutions: Option[Int]): SlotRequestResult = {
    val state = stateFor(key)
    state.monitor.synchronized {
      if(state.queue.nonEmpty || limitReached(state, maxParallelExecutions)) {
        val token = nextQueueToken()
        state.queue.enqueue(token)
        Queued(token)
      } else {
        state.runningExecutions += 1
        Acquired(new ActivityExecutionPermit(key))
      }
    }
  }

  // Converts a queued token into a permit only for the queue head and only if the current limit allows another running execution.
  def acquireQueued(key: ActivityLimiterKey, token: QueueToken, maxParallelExecutions: Option[Int]): Option[ActivityExecutionPermit] = {
    stateOption(key).flatMap { state =>
      state.monitor.synchronized {
        if(state.queue.headOption.contains(token) && !limitReached(state, maxParallelExecutions)) {
          state.queue.dequeue()
          state.runningExecutions += 1
          signalStateChanged(state)
          Some(new ActivityExecutionPermit(key))
        } else {
          None
        }
      }
    }
  }

  def cancelQueued(key: ActivityLimiterKey, token: QueueToken): Unit = {
    stateOption(key).foreach { state =>
      state.monitor.synchronized {
        state.queue.dequeueFirst(_ == token)
        signalStateChanged(state)
        cleanupIfIdle(key, state)
      }
    }
  }

  // Repeatedly tries to acquire a permit for a single caller, queueing once if needed and re-checking on wakeups, cancellations, and limit changes.
  def awaitPermit(key: ActivityLimiterKey,
                  currentLimit: => Option[Int],
                  isCancelled: () => Boolean,
                  timeoutMs: Long = 500L,
                  onQueuedTokenChanged: Option[QueueToken] => Unit = _ => ()): Option[ActivityExecutionPermit] = {
    var queuedToken: Option[QueueToken] = None
    var acquiredPermit = false

    try {
      while(!isCancelled()) {
        queuedToken match {
          case None =>
            requestSlot(key, currentLimit) match {
              case Acquired(permit) =>
                acquiredPermit = true
                return Some(permit)
              case Queued(token) =>
                queuedToken = Some(token)
                onQueuedTokenChanged(Some(token))
            }
          case Some(token) =>
            acquireQueued(key, token, currentLimit) match {
              case Some(permit) =>
                acquiredPermit = true
                queuedToken = None
                onQueuedTokenChanged(None)
                return Some(permit)
              case None =>
                val state = stateOption(key).getOrElse(return None)
                state.monitor.synchronized {
                  val tokenIsQueued = state.queue.headOption.contains(token)
                  if(!isCancelled() && tokenIsQueued && limitReached(state, currentLimit)) {
                    state.monitor.wait(timeoutMs)
                  }
                }
            }
        }
      }
      None
    } finally {
      if(!acquiredPermit) {
        queuedToken.foreach(token => cancelQueued(key, token))
        if(queuedToken.nonEmpty) {
          onQueuedTokenChanged(None)
        }
      }
    }
  }

  private[activity] def isTracked(key: ActivityLimiterKey): Boolean = this.synchronized {
    activityStates.contains(key)
  }

  private[activity] def queuedCount(key: ActivityLimiterKey): Int = {
    stateOption(key).map { state =>
      state.monitor.synchronized {
        state.queue.size
      }
    }.getOrElse(0)
  }

  // Releases a previously acquired permit, wakes queued waiters, and drops the whole key state once it becomes idle.
  private def release(key: ActivityLimiterKey): Unit = {
    stateOption(key).foreach { state =>
      state.monitor.synchronized {
        state.runningExecutions -= 1
        signalStateChanged(state)
        cleanupIfIdle(key, state)
      }
    }
  }

  private def stateFor(key: ActivityLimiterKey): ActivityExecutionState = this.synchronized {
    activityStates.getOrElseUpdate(key, ActivityExecutionState())
  }

  private def stateOption(key: ActivityLimiterKey): Option[ActivityExecutionState] = this.synchronized {
    activityStates.get(key)
  }

  private def nextQueueToken(): QueueToken = this.synchronized {
    nextQueueTokenId += 1
    QueueToken(nextQueueTokenId)
  }

  private def limitReached(state: ActivityExecutionState, maxParallelExecutions: Option[Int]): Boolean = {
    maxParallelExecutions.exists(limit => state.runningExecutions >= limit)
  }

  private def signalStateChanged(state: ActivityExecutionState): Unit = {
    state.monitor.notifyAll()
  }

  // Removes the in-memory state only if this exact state instance is still current, so stale cleanup from older races cannot remove a recreated queue.
  private def cleanupIfIdle(key: ActivityLimiterKey, state: ActivityExecutionState): Unit = {
    if(state.runningExecutions <= 0 && state.queue.isEmpty) {
      this.synchronized {
        activityStates.get(key).foreach { currentState =>
          if(currentState eq state) {
            activityStates.remove(key)
          }
        }
      }
    }
  }
}
