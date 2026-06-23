package org.silkframework.workspace.activity.workflow

/** Pure limiter state tests that exercise queue and permit behavior without activity execution. */
class WorkflowExecutionLimiterCoreTest extends WorkflowExecutionLimiterTestSupport {

  it should "grant queued workflow runs in FIFO order" in {
    val workflowTask = createLimitedWorkflow("fifo")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val initialPermit = WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
      case WorkflowExecutionLimiter.Acquired(permit) => permit
      case other => fail(s"Expected the first workflow run to acquire a permit immediately, but got: $other")
    }
    val queuedTokens = (1 to 3).map { _ =>
      WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
        case WorkflowExecutionLimiter.Queued(token) => token
        case other => fail(s"Expected queued workflow run, but got: $other")
      }
    }

    try {
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)) shouldBe None
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)) shouldBe None

      initialPermit.release()

      val secondPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens.head, Some(1)).getOrElse {
        fail("Expected the first queued workflow run to acquire the freed permit.")
      }
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)) shouldBe None
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)) shouldBe None

      secondPermit.release()

      val thirdPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)).getOrElse {
        fail("Expected the second queued workflow run to acquire the next freed permit.")
      }
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)) shouldBe None

      thirdPermit.release()

      val fourthPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)).getOrElse {
        fail("Expected the third queued workflow run to acquire the final freed permit.")
      }
      fourthPermit.release()

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      WorkflowExecutionLimiter.cancelQueued(workflowKey, queuedTokens.head)
      WorkflowExecutionLimiter.cancelQueued(workflowKey, queuedTokens(1))
      WorkflowExecutionLimiter.cancelQueued(workflowKey, queuedTokens(2))
    }
  }

  it should "honor a lowered limit while multiple runs are already queued" in {
    val workflowTask = createBlockingWorkflow("loweredLimit", maxParallelExecutions = Some(2))
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val firstPermit = WorkflowExecutionLimiter.requestSlot(workflowKey, Some(2)) match {
      case WorkflowExecutionLimiter.Acquired(permit) => permit
      case other => fail(s"Expected the first workflow run to acquire a permit immediately, but got: $other")
    }
    val secondPermit = WorkflowExecutionLimiter.requestSlot(workflowKey, Some(2)) match {
      case WorkflowExecutionLimiter.Acquired(permit) => permit
      case other => fail(s"Expected the second workflow run to acquire a permit immediately, but got: $other")
    }
    val queuedTokens = (1 to 2).map { _ =>
      WorkflowExecutionLimiter.requestSlot(workflowKey, Some(2)) match {
        case WorkflowExecutionLimiter.Queued(token) => token
        case other => fail(s"Expected queued workflow run after lowering-limit setup, but got: $other")
      }
    }

    try {
      firstPermit.release()
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens.head, Some(1)) shouldBe None
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)) shouldBe None

      secondPermit.release()
      val thirdPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens.head, Some(1)).getOrElse {
        fail("Expected the first queued workflow run to acquire a permit once both running executions completed.")
      }
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)) shouldBe None

      thirdPermit.release()
      val fourthPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(1), Some(1)).getOrElse {
        fail("Expected the second queued workflow run to acquire the final permit after the lowered-limit handoff.")
      }
      fourthPermit.release()

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      queuedTokens.foreach(token => WorkflowExecutionLimiter.cancelQueued(workflowKey, token))
    }
  }

  it should "advance FIFO order when a middle queued run is cancelled" in {
    val workflowTask = createLimitedWorkflow("middleCancel")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val initialPermit = WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
      case WorkflowExecutionLimiter.Acquired(permit) => permit
      case other => fail(s"Expected the first workflow run to acquire a permit immediately, but got: $other")
    }
    val queuedTokens = (1 to 3).map { _ =>
      WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
        case WorkflowExecutionLimiter.Queued(token) => token
        case other => fail(s"Expected queued workflow run, but got: $other")
      }
    }

    try {
      WorkflowExecutionLimiter.cancelQueued(workflowKey, queuedTokens(1))
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2

      initialPermit.release()
      val secondPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens.head, Some(1)).getOrElse {
        fail("Expected the first queued workflow run to acquire the freed permit after the middle waiter was cancelled.")
      }
      WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)) shouldBe None

      secondPermit.release()
      val thirdPermit = WorkflowExecutionLimiter.acquireQueued(workflowKey, queuedTokens(2), Some(1)).getOrElse {
        fail("Expected the last queued workflow run to advance after the earlier waiters completed or were cancelled.")
      }
      thirdPermit.release()

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      queuedTokens.foreach(token => WorkflowExecutionLimiter.cancelQueued(workflowKey, token))
    }
  }

  it should "ignore repeated permit releases" in {
    val workflowTask = createLimitedWorkflow("doubleRelease")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val permit = WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
      case WorkflowExecutionLimiter.Acquired(acquiredPermit) => acquiredPermit
      case other => fail(s"Expected the first workflow run to acquire a permit immediately, but got: $other")
    }

    permit.release()
    permit.release()

    WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    WorkflowExecutionLimiter.requestSlot(workflowKey, Some(1)) match {
      case WorkflowExecutionLimiter.Acquired(nextPermit) =>
        nextPermit.release()
      case other =>
        fail(s"Expected a fresh permit acquisition after repeated release, but got: $other")
    }
  }
}
