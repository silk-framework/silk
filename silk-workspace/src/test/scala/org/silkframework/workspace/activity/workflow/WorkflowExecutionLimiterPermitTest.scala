package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.{Activity, ActivityMonitor, Status}

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/** Permit-controlled and race-focused tests that isolate limiter handoff, restart, and cancellation behavior. */
class WorkflowExecutionLimiterPermitTest extends WorkflowExecutionLimiterTestSupport {

  it should "remove waiting workflow runs from the queue when they are cancelled" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("cancel")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:running"
    val queuedUser = "urn:test:queued"

    val firstControl = startAndAwaitPermitControlledWorkflow(workflowTask, runningUser, Some(workflowKey))
    val queuedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queuedUserContext = testUserContext(queuedUser)
    queuedControl.start()(queuedUserContext)

    try {
      expectQueuedCount(workflowKey, 1)
      queuedControl.cancel()(queuedUserContext)
      expectCancelled(queuedControl, "Expected queued workflow run to finish in cancelled state")
      expectQueuedCount(workflowKey, 0)
      PermitControlledWorkflowState.releaseForUser(runningUser)

      awaitFinished(firstControl)
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(Seq(firstControl, queuedControl)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }

  it should "treat restart of a queued workflow run as a fresh tail entry" in {
    PermitControlledWorkflowState.reset()
    QueuedPermitTokenState.reset()
    val workflowTask = createLimitedWorkflow("queuedRestart")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:queued-restart-running"
    val queuedUser = "urn:test:queued-restart-original"
    val otherQueuedUser = "urn:test:queued-restart-other"
    val restartedUser = "urn:test:queued-restart-restarted"

    val runningControl = startAndAwaitPermitControlledWorkflow(workflowTask, runningUser, Some(workflowKey))
    val restartedControl = startPermitControlledWorkflow(workflowTask, queuedUser)
    val otherQueuedControl = startPermitControlledWorkflow(workflowTask, otherQueuedUser)

    try {
      expectQueuedCount(workflowKey, 2)
      val originalQueuedToken = eventually {
        QueuedPermitTokenState.queuedTokenId(queuedUser).getOrElse {
          fail(s"Expected queued token for $queuedUser, but none was recorded yet.")
        }
      }
      val otherQueuedToken = eventually {
        QueuedPermitTokenState.queuedTokenId(otherQueuedUser).getOrElse {
          fail(s"Expected queued token for $otherQueuedUser, but none was recorded yet.")
        }
      }

      Await.result(restartTracked(restartedControl, restartedUser), TestTimeout)
      eventually {
        withClue(s"queuedTokens(original=$originalQueuedToken, other=$otherQueuedToken, restarted=${QueuedPermitTokenState.queuedTokenId(restartedUser)}), " +
          s"startedUsers=${PermitControlledWorkflowState.startedUserSeq}, ${limiterDebugString(workflowKey)} ") {
          val limiterState = WorkflowExecutionLimiter.debugState(workflowKey).getOrElse {
            fail(s"Expected limiter state for $workflowKey while validating queued restart ordering.")
          }
          val restartedQueuedToken = QueuedPermitTokenState.queuedTokenId(restartedUser).getOrElse {
            fail(s"Expected queued token for $restartedUser, but none was recorded yet.")
          }
          val queuedTokenIds = limiterState.prioritizedQueueTokenIds ++ limiterState.queueTokenIds
          queuedTokenIds shouldBe Seq(otherQueuedToken, restartedQueuedToken)
          queuedTokenIds should not contain originalQueuedToken
        }
      }
    } finally {
      cleanupControls(Seq(runningControl, restartedControl, otherQueuedControl)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }

  it should "move prioritized workflow starts to the head of the limiter queue" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("prioritizedFifo")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:prioritized-running"
    val queuedUser = "urn:test:prioritized-queued"
    val prioritizedUser = "urn:test:prioritized-head"

    val runningControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queuedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val prioritizedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))

    try {
      runningControl.start()(testUserContext(runningUser))
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      queuedControl.start()(testUserContext(queuedUser))
      expectQueuedCount(workflowKey, 1)

      prioritizedControl.startPrioritized()(testUserContext(prioritizedUser))
      expectQueuedCount(workflowKey, 2)

      PermitControlledWorkflowState.releaseForUser(runningUser)
      awaitFinished(runningControl)
      expectStartedUsers(PermitControlledWorkflowState, Seq(runningUser, prioritizedUser), Some(workflowKey))

      assertStartedExecutionsStay(PermitControlledWorkflowState, 2, 300.millis)

      PermitControlledWorkflowState.releaseForUser(prioritizedUser)
      awaitFinished(prioritizedControl)
      expectStartedUsers(PermitControlledWorkflowState, Seq(runningUser, prioritizedUser, queuedUser), Some(workflowKey))

      PermitControlledWorkflowState.releaseForUser(queuedUser)
      awaitFinished(queuedControl)
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(Seq(runningControl, queuedControl, prioritizedControl)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }

  it should "clean up queued executions if waiting for a slot is interrupted" in {
    val workflowTask = createLimitedWorkflow("interruptedQueueCleanup")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val permitExecutor = TestWorkflowPermitExecutor(workflowTask)
    val firstContext = new ActivityMonitor[WorkflowExecutionReport]("firstPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val waitingContext = new ActivityMonitor[WorkflowExecutionReport]("waitingPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val firstPermit = permitExecutor.acquirePermitForTests(firstContext).getOrElse {
      throw new AssertionError("Expected the first workflow execution to acquire a permit immediately.")
    }
    val waitingFailure = new AtomicReference[Throwable]()

    val waitingThread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          permitExecutor.acquirePermitForTests(waitingContext)
        } catch {
          case ex: Throwable =>
            waitingFailure.set(ex)
        }
      }
    })

    waitingThread.start()

    expectQueuedCount(workflowKey, 1)

    waitingThread.interrupt()
    waitingThread.join(TestTimeout.toMillis)

    try {
      waitingThread.isAlive shouldBe false
      waitingFailure.get() shouldBe a[InterruptedException]
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
    } finally {
      firstPermit()
    }
  }

  it should "clean up queued executions if their limiter state disappears before cancellation" in {
    val workflowTask = createLimitedWorkflow("missingStateCleanup")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val permitExecutor = TestWorkflowPermitExecutor(workflowTask)
    val firstContext = new ActivityMonitor[WorkflowExecutionReport]("missingStateFirstPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val waitingContext = new ActivityMonitor[WorkflowExecutionReport]("missingStateWaitingPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val firstPermit = permitExecutor.acquirePermitForTests(firstContext).getOrElse {
      throw new AssertionError("Expected the first workflow execution to acquire a permit immediately.")
    }
    val waitingFailure = new AtomicReference[Throwable]()

    val waitingThread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          permitExecutor.acquirePermitForTests(waitingContext)
        } catch {
          case ex: Throwable =>
            waitingFailure.set(ex)
        }
      }
    })

    waitingThread.start()

    expectQueuedCount(workflowKey, 1)

    WorkflowExecutionLimiter.removeStateForTests(workflowKey)
    waitingThread.interrupt()
    waitingThread.join(TestTimeout.toMillis)

    try {
      waitingThread.isAlive shouldBe false
      waitingFailure.get() shouldBe a[InterruptedException]
      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
    } finally {
      firstPermit()
    }
  }

  it should "treat restart during queue-to-running handoff as a fresh queued start" in {
    PermitControlledWorkflowState.reset()
    QueuedPermitTokenState.reset()
    val workflowTask = createLimitedWorkflow("restartHandoff")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val hookEntered = new CountDownLatch(1)
    val allowAcquire = new CountDownLatch(1)
    val originalUser = "urn:test:restart-handoff-original"
    val runningUser = "urn:test:restart-handoff-running"
    val restartedUser = "urn:test:restart-handoff-restarted"
    val otherQueuedUser = "urn:test:restart-handoff-other"
    val runningControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val control = Activity(RestartQueuedWorkflowExecutor(workflowTask, hookEntered, allowAcquire))
    val otherQueuedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))

    try {
      runningControl.start()(testUserContext(runningUser))
      expectStartedUsers(PermitControlledWorkflowState, Seq(runningUser), Some(workflowKey))
      control.start()(testUserContext(originalUser))
      otherQueuedControl.start()(testUserContext(otherQueuedUser))
      expectQueuedCount(workflowKey, 2)
      val originalQueuedToken = eventually {
        QueuedPermitTokenState.queuedTokenId(originalUser).getOrElse {
          fail(s"Expected queued token for $originalUser, but none was recorded yet.")
        }
      }
      val otherQueuedToken = eventually {
        QueuedPermitTokenState.queuedTokenId(otherQueuedUser).getOrElse {
          fail(s"Expected queued token for $otherQueuedUser, but none was recorded yet.")
        }
      }

      PermitControlledWorkflowState.releaseForUser(runningUser)
      if(!hookEntered.await(10, TimeUnit.SECONDS)) {
        fail("Timed out waiting for the queued workflow run to reach the queue-to-running handoff.")
      }

      // Pause the original queued run after it has been selected for handoff, but before it can finish permit
      // acquisition. This exposes the narrow window in which restart() must replace that run with a fresh queue entry.
      val restartFuture = restartTracked(control, restartedUser)
      allowAcquire.countDown()

      Await.result(restartFuture, TestTimeout)
      eventually {
        withClue(s"queuedTokens(original=$originalQueuedToken, other=$otherQueuedToken, restarted=${QueuedPermitTokenState.queuedTokenId(restartedUser)}), " +
          s"startedUsers=${PermitControlledWorkflowState.startedUserSeq}, ${limiterDebugString(workflowKey)} ") {
          val limiterState = WorkflowExecutionLimiter.debugState(workflowKey).getOrElse {
            fail(s"Expected limiter state for $workflowKey while validating the restart handoff queue ordering.")
          }
          val restartedQueuedToken = QueuedPermitTokenState.queuedTokenId(restartedUser).getOrElse {
            fail(s"Expected queued token for $restartedUser, but none was recorded yet.")
          }
          val queuedTokenIds = limiterState.prioritizedQueueTokenIds ++ limiterState.queueTokenIds
          PermitControlledWorkflowState.startedUserSet should not contain originalUser
          queuedTokenIds should not contain originalQueuedToken
          queuedTokenIds should contain (restartedQueuedToken)
          if(queuedTokenIds.contains(otherQueuedToken)) {
            queuedTokenIds.indexOf(otherQueuedToken) should be < queuedTokenIds.indexOf(restartedQueuedToken)
          } else {
            PermitControlledWorkflowState.startedUserSet should contain (otherQueuedUser)
          }
        }
      }
    } finally {
      cleanupControls(Seq(runningControl, control, otherQueuedControl)) {
        PermitControlledWorkflowState.releaseAll()
        allowAcquire.countDown()
      }
    }
  }

  it should "collapse overlapping queued restarts into a single replacement execution" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("overlappingRestart")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val firstPermitExecutor = TestWorkflowPermitExecutor(workflowTask)
    val firstContext = new ActivityMonitor[WorkflowExecutionReport]("overlappingRestartFirstPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val firstPermit = firstPermitExecutor.acquirePermitForTests(firstContext).getOrElse {
      throw new AssertionError("Expected the first workflow execution to acquire a permit immediately.")
    }
    val control = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val restartUsers = Set("urn:test:overlapping-restart-1", "urn:test:overlapping-restart-2")

    try {
      control.start()(testUserContext("urn:test:overlapping-original"))
      expectQueuedCount(workflowKey, 1)

      val restarts = Future.sequence(restartUsers.toSeq.map(userUri => restartTracked(control, userUri)))
      expectQueuedCount(workflowKey, 1)

      firstPermit()
      Await.result(restarts, TestTimeout)
      expectStartedCount(PermitControlledWorkflowState, 1, Some(workflowKey))
      val startedUsers = PermitControlledWorkflowState.startedUserSeq
      startedUsers should have size 1
      restartUsers should contain (startedUsers.head)

      PermitControlledWorkflowState.releaseNext()
      awaitFinished(control)
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(Seq(control)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }

  it should "not start the workflow body if cancel races with queued permit acquisition" in {
    val workflowTask = createLimitedWorkflow("cancelRace")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val firstContext = new ActivityMonitor[WorkflowExecutionReport]("cancelRaceFirstPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val firstPermitExecutor = TestWorkflowPermitExecutor(workflowTask)
    val firstPermit = firstPermitExecutor.acquirePermitForTests(firstContext).getOrElse {
      throw new AssertionError("Expected the first workflow execution to acquire a permit immediately.")
    }
    val hookEntered = new CountDownLatch(1)
    val allowAcquire = new CountDownLatch(1)
    val startedBodies = new AtomicInteger(0)
    val control = Activity(CancelRaceWorkflowExecutor(workflowTask, hookEntered, allowAcquire, startedBodies))
    val queuedUserContext = testUserContext("urn:test:cancel-race")

    try {
      control.start()(queuedUserContext)
      expectQueuedCount(workflowKey, 1)

      firstPermit()
      if(!hookEntered.await(10, TimeUnit.SECONDS)) {
        fail("Timed out waiting for the queued workflow run to reach the permit-acquisition handoff.")
      }

      control.cancel()(queuedUserContext)
      allowAcquire.countDown()
      awaitFinished(control)

      startedBodies.get() shouldBe 0
      expectCancelled(control, "Expected the cancelled workflow run to finish in cancelled state")
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cleanupControls(Seq(control)) {
        allowAcquire.countDown()
      }
    }
  }

  it should "clean up queued executions if limit lookup fails while waiting" in {
    val workflowTask = createLimitedWorkflow("limitLookupFailure")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val firstContext = new ActivityMonitor[WorkflowExecutionReport]("limitLookupFailureFirstPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val firstPermitExecutor = TestWorkflowPermitExecutor(workflowTask)
    val firstPermit = firstPermitExecutor.acquirePermitForTests(firstContext).getOrElse {
      throw new AssertionError("Expected the first workflow execution to acquire a permit immediately.")
    }
    val failLimitLookup = new AtomicReference[Option[TestFailureException]](None)
    val permitExecutor = FailingLimitLookupWorkflowExecutor(workflowTask, failLimitLookup)
    val waitingContext = new ActivityMonitor[WorkflowExecutionReport]("limitLookupFailureWaitingPermit", initialValue = Some(WorkflowExecutionReport(workflowTask)))
    val waitingFailure = new AtomicReference[Throwable]()

    val waitingThread = new Thread(new Runnable {
      override def run(): Unit = {
        try {
          permitExecutor.acquirePermitForTests(waitingContext)
        } catch {
          case ex: Throwable =>
            waitingFailure.set(ex)
        }
      }
    })

    waitingThread.start()

    expectQueuedCount(workflowKey, 1)

    val expectedFailure = TestFailureException("current workflow execution limit lookup failed")
    failLimitLookup.set(Some(expectedFailure))

    eventually {
      waitingThread.isAlive shouldBe false
      waitingFailure.get() shouldBe expectedFailure
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
    }

    try {
      firstPermit()
      expectUntracked(workflowKey)
    } finally {
      waitingThread.join(TestTimeout.toMillis)
    }
  }

  it should "isolate workflow queues under concurrent pressure" in {
    PermitControlledWorkflowState.reset()
    val workflowTaskA = createLimitedWorkflow("isolationA")
    val workflowTaskB = createLimitedWorkflow("isolationB")
    val workflowKeyA = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTaskA.project.id, workflowTaskA.id)
    val workflowKeyB = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTaskB.project.id, workflowTaskB.id)
    val a1 = Activity(PermitControlledWorkflowExecutor(workflowTaskA))
    val a2 = Activity(PermitControlledWorkflowExecutor(workflowTaskA))
    val b1 = Activity(PermitControlledWorkflowExecutor(workflowTaskB))
    val b2 = Activity(PermitControlledWorkflowExecutor(workflowTaskB))

    try {
      a1.start()(testUserContext("urn:test:isolation-a1"))
      b1.start()(testUserContext("urn:test:isolation-b1"))
      expectStartedUserSet(PermitControlledWorkflowState, Set("urn:test:isolation-a1", "urn:test:isolation-b1"), Some(workflowKeyA))
      a2.start()(testUserContext("urn:test:isolation-a2"))
      b2.start()(testUserContext("urn:test:isolation-b2"))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKeyA) shouldBe 1
        WorkflowExecutionLimiter.queuedCount(workflowKeyB) shouldBe 1
      }

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-a1")
      awaitFinished(a1)
      expectStartedCount(PermitControlledWorkflowState, 3, Some(workflowKeyA))
      PermitControlledWorkflowState.startedUserSet should contain ("urn:test:isolation-a2")
      WorkflowExecutionLimiter.queuedCount(workflowKeyB) shouldBe 1
      assertStartedExecutionsStay(PermitControlledWorkflowState, 3, 300.millis)

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-a2")
      awaitFinished(a2)
      assertStartedExecutionsStay(PermitControlledWorkflowState, 3, 300.millis)

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-b1")
      awaitFinished(b1)
      expectStartedCount(PermitControlledWorkflowState, 4, Some(workflowKeyB))
      PermitControlledWorkflowState.startedUserSet should contain ("urn:test:isolation-b2")

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-b2")
      awaitFinished(b2)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKeyA) shouldBe false
        WorkflowExecutionLimiter.isTracked(workflowKeyB) shouldBe false
      }
    } finally {
      cleanupControls(Seq(a1, a2, b1, b2)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }

  it should "treat repeated queued cancellations as harmless" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("doubleCancel")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:double-cancel-running"
    val runningControl = startAndAwaitPermitControlledWorkflow(workflowTask, runningUser, Some(workflowKey))
    val queuedUser = "urn:test:double-cancel-queued"
    val queuedControl = startPermitControlledWorkflow(workflowTask, queuedUser)

    try {
      expectQueuedCount(workflowKey, 1)

      queuedControl.cancel()(testUserContext(queuedUser))
      queuedControl.cancel()(testUserContext(queuedUser))
      expectCancelled(queuedControl, "Expected the repeatedly cancelled queued workflow run to finish cancelled")
      expectQueuedCount(workflowKey, 0)

      PermitControlledWorkflowState.releaseForUser(runningUser)
      awaitFinished(runningControl)
      expectUntracked(workflowKey)
    } finally {
      cleanupControls(Seq(runningControl, queuedControl)) {
        PermitControlledWorkflowState.releaseAll()
      }
    }
  }
}
