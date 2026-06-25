package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.{Activity, ActivityExecution}
import org.silkframework.runtime.plugin.types.IntOptionParameter

import scala.concurrent.duration._

/** Activity-level integration tests that run real local workflow executions against the limiter. */
class WorkflowExecutionLimiterActivityTest extends WorkflowExecutionLimiterTestSupport {

  it should "re-evaluate the workflow limit while waiting" in {
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("reconfigure")

    val firstControl = Activity(LocalWorkflowExecutor(workflowTask))
    firstControl.start()(testUserContext("urn:test:first"))

    val secondControl = Activity(LocalWorkflowExecutor(workflowTask))
    secondControl.start()(testUserContext("urn:test:second"))

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(2))))
      expectStartedCount(QueueControlledTaskState, 2)

      QueueControlledTaskState.releaseNext()
      QueueControlledTaskState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      expectStartedUserSet(QueueControlledTaskState, Set("urn:test:first", "urn:test:second"))
    } finally {
      cleanupControls(Seq(firstControl, secondControl)) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  it should "allow queued workflow runs to proceed when the limit is removed" in {
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("removeLimit")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val userUris = Seq("urn:test:remove-1", "urn:test:remove-2")
    val controls = userUris.map(userUri => startWorkflow(workflowTask, userUri))

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      expectQueuedCount(workflowKey, 1)

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(None)))
      expectStartedUsers(QueueControlledTaskState, userUris)

      QueueControlledTaskState.releaseNext()
      QueueControlledTaskState.releaseNext()
      controls.foreach(awaitFinished)
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(controls) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  it should "bypass the limiter completely for unlimited workflows" in {
    QueueControlledTaskState.reset()
    val workflowTask = createUnlimitedWorkflow("unlimited")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)

    val userUris = (1 to 4).map(index => s"urn:test:unlimited-$index")
    val controls = userUris.map(userUri => startWorkflow(workflowTask, userUri))

    try {
      QueueControlledTaskState.awaitStartedExecutions(4)
      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false

      (1 to controls.size).foreach(_ => QueueControlledTaskState.releaseNext())
      controls.foreach(awaitFinished)
      expectStartedUserSet(QueueControlledTaskState, userUris.toSet)

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      cleanupControls(controls) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  it should "clean up tracked state when all queued waiters are cancelled" in {
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("cancelAllQueued")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:cancel-all-running"
    val queuedUsers = Seq("urn:test:cancel-all-1", "urn:test:cancel-all-2", "urn:test:cancel-all-3")

    val runningControl = startWorkflow(workflowTask, runningUser)
    val queuedControls = queuedUsers.map(userUri => userUri -> startWorkflow(workflowTask, userUri))

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      expectQueuedCount(workflowKey, queuedControls.size)

      queuedControls.foreach { case (userUri, control) =>
        control.cancel()(testUserContext(userUri))
      }
      queuedControls.foreach { case (userUri, control) =>
        expectCancelled(control, s"Expected queued workflow run '$userUri' to finish cancelled")
      }
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe true
      }

      QueueControlledTaskState.releaseNext()
      awaitFinished(runningControl)
      expectStartedUsers(QueueControlledTaskState, Seq(runningUser))
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(runningControl +: queuedControls.map(_._2)) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  it should "keep already started runs unaffected when the limit is lowered again" in {
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("lowerAfterStart")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val userUris = Seq("urn:test:lower-after-1", "urn:test:lower-after-2", "urn:test:lower-after-3")
    val firstControl = startWorkflow(workflowTask, userUris.head)
    val secondControl = startWorkflow(workflowTask, userUris(1))

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      expectQueuedCount(workflowKey, 1)

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(2))))
      expectStartedUserSet(QueueControlledTaskState, userUris.take(2).toSet, Some(workflowKey))

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(1))))
      val thirdControl = startWorkflow(workflowTask, userUris(2))
      try {
        expectQueuedCount(workflowKey, 1)

        QueueControlledTaskState.releaseNext()
        awaitFinished(firstControl)
        assertStartedExecutionsStay(QueueControlledTaskState, 2, 800.millis)
        withClue(s"${limiterDebugString(workflowKey)} ") {
          WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
        }

        QueueControlledTaskState.releaseNext()
        awaitFinished(secondControl)
        expectStartedUserSet(QueueControlledTaskState, userUris.toSet, Some(workflowKey))

        QueueControlledTaskState.releaseNext()
        awaitFinished(thirdControl)
        expectUntracked(workflowKey)
      } finally {
        cleanupControls(Seq(thirdControl))()
      }
    } finally {
      cleanupControls(Seq(firstControl, secondControl)) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  it should "not block the activity thread pool while workflow runs are waiting for a slot" in {
    QueueControlledTaskState.reset()
    QuickTaskState.reset()
    val limitedWorkflowTask = createLimitedWorkflow("pool")
    val quickWorkflowTask = createQuickWorkflow("poolQuick")
    val parallelism = ActivityExecution.forkJoinPool.getParallelism

    val limitedControls =
      (1 to parallelism).map { index =>
        val control = Activity(LocalWorkflowExecutor(limitedWorkflowTask))
        control.start()(testUserContext(s"urn:test:pool-$index"))
        control
      }

    QueueControlledTaskState.awaitStartedExecutions(1)

    val quickControl = Activity(LocalWorkflowExecutor(quickWorkflowTask))
    quickControl.start()(testUserContext("urn:test:quick"))

    try {
      eventually {
        QuickTaskState.executions.get() shouldBe 1
      }

      limitedControls.tail.foreach(_.cancel()(testUserContext("urn:test:cancel")))
      QueueControlledTaskState.releaseNext()

      awaitFinished(limitedControls.head)
      limitedControls.tail.zipWithIndex.foreach { case (control, index) =>
        expectCancelled(control, s"Expected cancelled queued workflow run ${index + 1} to finish cancelled")
      }
      awaitFinished(quickControl)

      QueueControlledTaskState.startedExecutions.get() shouldBe 1
      QuickTaskState.executions.get() shouldBe 1
    } finally {
      cleanupControls(limitedControls :+ quickControl) {
        QueueControlledTaskState.releaseAll()
      }
    }
  }

  // FIXME: This is a known restriction of the workflow limiter currently. When many (multiple per second) short-running (> 500ms) instances
  //  of the same workflow are started, the waiting overhead becomes significant.
  it should "handoff queued fast workflows without poll-interval delay when the pool is otherwise idle" ignore {
    QuickTaskState.reset()
    val workflowTask = createQuickWorkflow("fastQueue", maxParallelExecutions = Some(1))

    val controls =
      (1 to 3).map { index =>
        val control = Activity(LocalWorkflowExecutor(workflowTask))
        control.start()(testUserContext(s"urn:test:fast-$index"))
        control
      }

    try {
      controls.foreach(awaitFinished)

      QuickTaskState.executions.get() shouldBe 3
      val startTimes = QuickTaskState.executionStartTimes()
      startTimes should have size 3

      val maxGapMillis = startTimes
        .sliding(2)
        .map {
          case Seq(previous, next) => (next - previous).nanos.toMillis
          case other => fail(s"Unexpected start time window: $other")
        }
        .max

      withClue(s"Expected quick workflow handoff gaps to stay well below the 500 ms blockUntil poll interval, but saw $maxGapMillis ms.") {
        maxGapMillis should be < 400L
      }
    } finally {
      cleanupControls(controls)()
    }
  }
}
