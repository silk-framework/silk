package org.silkframework.workspace.activity.workflow

import org.silkframework.util.Identifier

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable

/**
  * Coordinates per-workflow execution limits across all workflow starts.
  *
  * The limiter keeps one in-memory state bucket per workflow task. Each bucket tracks the number of currently
  * running executions and waiting queues for prioritized and normal starts. A workflow start either acquires a slot
  * immediately or gets a queue token that represents its waiting position. Waiting executions do not block activity
  * pool threads themselves. Instead, the workflow executor uses the queue token together with
  * `ActivityContext.blockUntil` and periodically retries acquisition once the queued execution reaches the effective
  * queue head and the configured limit allows it. Prioritized starts are admitted before normal queued starts while
  * keeping FIFO order within each priority lane.
  *
  * Limits are re-evaluated on every acquisition attempt, so runtime configuration changes affect only new starts
  * and executions that are still waiting for a slot.
  */
private[workflow] object WorkflowExecutionLimiter {

  final case class WorkflowExecutionKey(projectId: Identifier, workflowId: Identifier)

  final case class QueueToken private(id: Long)

  /** Test-only snapshot of a workflow limiter bucket. */
  private[workflow] final case class WorkflowExecutionDebugState(runningExecutions: Int,
                                                                 prioritizedQueueTokenIds: Seq[Long],
                                                                 queueTokenIds: Seq[Long],
                                                                 headTokenId: Option[Long]) {
    def queuedCount: Int = prioritizedQueueTokenIds.size + queueTokenIds.size
  }

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
                                            prioritizedQueue: mutable.Queue[QueueToken] = mutable.Queue.empty,
                                            queue: mutable.Queue[QueueToken] = mutable.Queue.empty,
                                            monitor: AnyRef = new Object()) {
    def headOption: Option[QueueToken] = prioritizedQueue.headOption.orElse(queue.headOption)

    def dequeueHead(): QueueToken = {
      if(prioritizedQueue.nonEmpty) {
        prioritizedQueue.dequeue()
      } else {
        queue.dequeue()
      }
    }

    def removeQueued(token: QueueToken): Unit = {
      if(!prioritizedQueue.dequeueFirst(_ == token).isDefined) {
        queue.dequeueFirst(_ == token)
      }
    }

    def queuedCount: Int = prioritizedQueue.size + queue.size

    def hasQueuedExecutions: Boolean = prioritizedQueue.nonEmpty || queue.nonEmpty
  }

  private val workflowStates = mutable.Map.empty[WorkflowExecutionKey, WorkflowExecutionState]
  private var nextQueueTokenId = 0L

  /**
    * Tries to acquire a slot for a new workflow start or enqueues it in the workflow wait queue.
    *
    * Prioritized starts still respect the workflow-specific execution limit, but they enter ahead of normal queued
    * starts. FIFO order is preserved within each queue lane.
    */
  def requestSlot(key: WorkflowExecutionKey,
                  maxParallelExecutions: Option[Int],
                  prioritized: Boolean = false): SlotRequestResult = {
    val state = stateFor(key)
    state.monitor.synchronized {
      if(state.hasQueuedExecutions || limitReached(state, maxParallelExecutions)) {
        val token = nextQueueToken()
        if(prioritized) {
          state.prioritizedQueue.enqueue(token)
        } else {
          state.queue.enqueue(token)
        }
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
        state.headOption.contains(token) && !limitReached(state, maxParallelExecutions)
      }
    }
  }

  /** Acquires the slot for a queued execution once it is eligible to leave the FIFO queue. */
  def acquireQueued(key: WorkflowExecutionKey, token: QueueToken, maxParallelExecutions: Option[Int]): Option[WorkflowExecutionPermit] = {
    stateOption(key).flatMap { state =>
      state.monitor.synchronized {
        if(state.headOption.contains(token) && !limitReached(state, maxParallelExecutions)) {
          state.dequeueHead()
          state.runningExecutions += 1
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
        state.removeQueued(token)
        cleanupIfIdle(key, state)
      }
    }
  }

  /** Visible for tests that need to verify whether a specific workflow is currently tracked by the limiter. */
  private[workflow] def isTracked(key: WorkflowExecutionKey): Boolean = this.synchronized {
    workflowStates.contains(key)
  }

  /** Visible for tests that need to verify that waiting executions are fully cleaned up. */
  private[workflow] def queuedCount(key: WorkflowExecutionKey): Int = {
    stateOption(key).map { state =>
      state.monitor.synchronized {
        state.queuedCount
      }
    }.getOrElse(0)
  }

  /** Visible for tests that need to inspect exact queue state and token order. */
  private[workflow] def debugState(key: WorkflowExecutionKey): Option[WorkflowExecutionDebugState] = {
    stateOption(key).map { state =>
      state.monitor.synchronized {
        WorkflowExecutionDebugState(
          runningExecutions = state.runningExecutions,
          prioritizedQueueTokenIds = state.prioritizedQueue.iterator.map(_.id).toSeq,
          queueTokenIds = state.queue.iterator.map(_.id).toSeq,
          headTokenId = state.headOption.map(_.id)
        )
      }
    }
  }

  /**
    * Test-only helper that removes a workflow state bucket explicitly.
    *
    * Tests use this to simulate rare race conditions deterministically, e.g. the state disappearing while a queued
    * execution still holds a queue token. Production code must never call this.
    */
  private[workflow] def removeStateForTests(key: WorkflowExecutionKey): Unit = this.synchronized {
    workflowStates.remove(key)
  }

  private def release(key: WorkflowExecutionKey): Unit = {
    stateOption(key).foreach { state =>
      state.monitor.synchronized {
        state.runningExecutions -= 1
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

  private def cleanupIfIdle(key: WorkflowExecutionKey, state: WorkflowExecutionState): Unit = {
    if(state.runningExecutions <= 0 && !state.hasQueuedExecutions) {
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
