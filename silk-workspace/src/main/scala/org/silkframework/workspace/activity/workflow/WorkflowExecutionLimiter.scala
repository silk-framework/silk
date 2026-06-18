package org.silkframework.workspace.activity.workflow

import org.silkframework.util.Identifier

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable

/**
  * Coordinates per-workflow execution limits across all workflow starts.
  *
  * The limiter keeps one in-memory state bucket per workflow task. Each bucket tracks the number of currently
  * running executions and a FIFO queue of waiting executions. A workflow start either acquires a slot immediately
  * or gets a queue token that represents its waiting position. Waiting executions do not block activity pool
  * threads themselves. Instead, the workflow executor uses the queue token together with `ActivityContext.blockUntil`
  * and retries acquisition once the queued execution reaches the queue head and the configured limit allows it.
  *
  * Limits are re-evaluated on every acquisition attempt, so runtime configuration changes affect only new starts
  * and executions that are still waiting for a slot.
  */
private[workflow] object WorkflowExecutionLimiter {

  final case class WorkflowExecutionKey(projectId: Identifier, workflowId: Identifier)

  final case class QueueToken private(id: Long)

  sealed trait SlotRequestResult

  final case class Acquired(permit: WorkflowExecutionPermit) extends SlotRequestResult

  final case class Queued(token: QueueToken) extends SlotRequestResult

  final class WorkflowExecutionPermit private[workflow](private val key: WorkflowExecutionKey) {
    private val released = new AtomicBoolean(false)

    def release(): Unit = {
      if(released.compareAndSet(false, true)) {
        WorkflowExecutionLimiter.release(key)
      }
    }
  }

  private case class WorkflowExecutionState(var runningExecutions: Int = 0,
                                            queue: mutable.Queue[QueueToken] = mutable.Queue.empty,
                                            monitor: AnyRef = new Object())

  private val workflowStates = mutable.Map.empty[WorkflowExecutionKey, WorkflowExecutionState]
  private var nextQueueTokenId = 0L

  /** Tries to acquire a slot for a new workflow start or enqueues it at the tail of the FIFO wait queue. */
  def requestSlot(key: WorkflowExecutionKey, maxParallelExecutions: Option[Int]): SlotRequestResult = {
    val state = stateFor(key)
    state.monitor.synchronized {
      if(state.queue.nonEmpty || limitReached(state, maxParallelExecutions)) {
        val token = nextQueueToken()
        state.queue.enqueue(token)
        signalStateChanged(state)
        Queued(token)
      } else {
        state.runningExecutions += 1
        Acquired(new WorkflowExecutionPermit(key))
      }
    }
  }

  /** Checks if a queued execution is now allowed to proceed, i.e. it reached the queue head and a slot is free. */
  def canAcquireQueued(key: WorkflowExecutionKey, token: QueueToken, maxParallelExecutions: Option[Int]): Boolean = {
    stateOption(key).exists { state =>
      state.monitor.synchronized {
        state.queue.headOption.contains(token) && !limitReached(state, maxParallelExecutions)
      }
    }
  }

  /** Acquires the slot for a queued execution once it is eligible to leave the FIFO queue. */
  def acquireQueued(key: WorkflowExecutionKey, token: QueueToken, maxParallelExecutions: Option[Int]): Option[WorkflowExecutionPermit] = {
    stateOption(key).flatMap { state =>
      state.monitor.synchronized {
        if(state.queue.headOption.contains(token) && !limitReached(state, maxParallelExecutions)) {
          state.queue.dequeue()
          state.runningExecutions += 1
          signalStateChanged(state)
          Some(new WorkflowExecutionPermit(key))
        } else {
          None
        }
      }
    }
  }

  /** Removes a waiting execution from the queue, e.g. after cancellation while it has not started yet. */
  def cancelQueued(key: WorkflowExecutionKey, token: QueueToken): Unit = {
    stateOption(key).foreach { state =>
      state.monitor.synchronized {
        state.queue.dequeueFirst(_ == token)
        signalStateChanged(state)
        cleanupIfIdle(key, state)
      }
    }
  }

  /** Returns the monitor for a workflow queue wait. Waiters use timeout-backed waits on this monitor. */
  def waitMonitor(key: WorkflowExecutionKey): Option[AnyRef] = {
    stateOption(key).map(_.monitor)
  }

  /** Visible for tests that need to verify whether a specific workflow is currently tracked by the limiter. */
  private[workflow] def isTracked(key: WorkflowExecutionKey): Boolean = this.synchronized {
    workflowStates.contains(key)
  }

  /** Visible for tests that need to verify that waiting executions are fully cleaned up. */
  private[workflow] def queuedCount(key: WorkflowExecutionKey): Int = {
    stateOption(key).map { state =>
      state.monitor.synchronized {
        state.queue.size
      }
    }.getOrElse(0)
  }

  private def release(key: WorkflowExecutionKey): Unit = {
    stateOption(key).foreach { state =>
      state.monitor.synchronized {
        state.runningExecutions -= 1
        signalStateChanged(state)
        cleanupIfIdle(key, state)
      }
    }
  }

  private def stateFor(key: WorkflowExecutionKey): WorkflowExecutionState = this.synchronized {
    workflowStates.getOrElseUpdate(key, WorkflowExecutionState())
  }

  private def stateOption(key: WorkflowExecutionKey): Option[WorkflowExecutionState] = this.synchronized {
    workflowStates.get(key)
  }

  private def nextQueueToken(): QueueToken = this.synchronized {
    nextQueueTokenId += 1
    QueueToken(nextQueueTokenId)
  }

  private def limitReached(state: WorkflowExecutionState, maxParallelExecutions: Option[Int]): Boolean = {
    maxParallelExecutions.exists(limit => state.runningExecutions >= limit)
  }

  private def signalStateChanged(state: WorkflowExecutionState): Unit = {
    state.monitor.notifyAll()
  }

  private def cleanupIfIdle(key: WorkflowExecutionKey, state: WorkflowExecutionState): Unit = {
    if(state.runningExecutions <= 0 && state.queue.isEmpty) {
      this.synchronized {
        workflowStates.get(key).foreach { currentState =>
          if(currentState eq state) {
            workflowStates.remove(key)
          }
        }
      }
    }
  }
}
