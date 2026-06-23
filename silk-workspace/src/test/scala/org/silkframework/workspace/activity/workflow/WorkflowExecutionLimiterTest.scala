package org.silkframework.workspace.activity.workflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.silkframework.config.{CustomTask, InputPorts, Port, Task}
import org.silkframework.dataset.Dataset
import org.silkframework.execution.{ExecutionReport, ExecutionType, Executor, ExecutorOutput}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.types.IntOptionParameter
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.users.{User, UserActions}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TestWorkspaceProviderTestTrait

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters._

class WorkflowExecutionLimiterTest extends AnyFlatSpec with Matchers with Eventually with TestWorkspaceProviderTestTrait with BeforeAndAfterAll with TestUserContextTrait {
  private val TestTimeout: FiniteDuration = 25.seconds

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(TestTimeout.toSeconds, Seconds)))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[QueueControlledTask])
    PluginRegistry.registerPlugin(classOf[QueueControlledTaskExecutor])
    PluginRegistry.registerPlugin(classOf[QuickTask])
    PluginRegistry.registerPlugin(classOf[QuickTaskExecutor])
  }

  behavior of "WorkflowExecutionLimiter"

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

  it should "remove waiting workflow runs from the queue when they are cancelled" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("cancel")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:running"
    val queuedUser = "urn:test:queued"

    val firstControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    firstControl.start()(testUserContext(runningUser))
    val queuedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queuedUserContext = testUserContext(queuedUser)
    queuedControl.start()(queuedUserContext)

    try {
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }
      queuedControl.cancel()(queuedUserContext)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
      }
      PermitControlledWorkflowState.releaseForUser(runningUser)

      awaitFinished(firstControl)
      eventually {
        queuedControl.status() match {
          case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
          case other => fail(s"Expected queued workflow run to finish in cancelled state, but got: $other")
        }
      }
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(firstControl, queuedControl))
      PermitControlledWorkflowState.releaseAll()
    }
  }

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
      QueueControlledTaskState.awaitStartedExecutions(2)

      QueueControlledTaskState.releaseNext()
      QueueControlledTaskState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      QueueControlledTaskState.startedExecutions.get() shouldBe 2
      QueueControlledTaskState.startedUsers.asScala.toSet shouldBe Set("urn:test:first", "urn:test:second")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledTaskState.releaseAll()
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

  it should "allow queued workflow runs to proceed when the limit is removed" in {
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("removeLimit")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val userUris = Seq("urn:test:remove-1", "urn:test:remove-2")
    val controls = userUris.map(userUri => startWorkflow(workflowTask, userUri))

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(None)))
      QueueControlledTaskState.awaitStartedExecutions(2)
      QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe userUris

      QueueControlledTaskState.releaseNext()
      QueueControlledTaskState.releaseNext()
      controls.foreach(awaitFinished)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(controls)
      QueueControlledTaskState.releaseAll()
    }
  }

  it should "treat restart of a queued workflow run as a fresh tail entry" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("queuedRestart")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:queued-restart-running"
    val queuedUser = "urn:test:queued-restart-original"
    val otherQueuedUser = "urn:test:queued-restart-other"
    val restartedUser = "urn:test:queued-restart-restarted"

    val runningControl = startPermitControlledWorkflow(workflowTask, runningUser)
    val restartedControl = startPermitControlledWorkflow(workflowTask, queuedUser)
    val otherQueuedControl = startPermitControlledWorkflow(workflowTask, otherQueuedUser)

    try {
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }

      Await.result(restartedControl.restart()(testUserContext(restartedUser)), TestTimeout)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }

      PermitControlledWorkflowState.releaseForUser(runningUser)
      awaitFinished(runningControl)
      PermitControlledWorkflowState.awaitStartedExecutions(2)
      PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq(runningUser, otherQueuedUser)

      PermitControlledWorkflowState.releaseForUser(otherQueuedUser)
      awaitFinished(otherQueuedControl)
      PermitControlledWorkflowState.awaitStartedExecutions(3)
      PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq(runningUser, otherQueuedUser, restartedUser)

      PermitControlledWorkflowState.releaseForUser(restartedUser)
      awaitFinished(restartedControl)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(runningControl, restartedControl, otherQueuedControl))
      PermitControlledWorkflowState.releaseAll()
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
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      prioritizedControl.startPrioritized()(testUserContext(prioritizedUser))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }

      PermitControlledWorkflowState.releaseForUser(runningUser)
      awaitFinished(runningControl)
      PermitControlledWorkflowState.awaitStartedExecutions(2)
      PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq(runningUser, prioritizedUser)

      assertPermitControlledStartsStay(2, 300.millis)

      PermitControlledWorkflowState.releaseForUser(prioritizedUser)
      awaitFinished(prioritizedControl)
      PermitControlledWorkflowState.awaitStartedExecutions(3)
      PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq(runningUser, prioritizedUser, queuedUser)

      PermitControlledWorkflowState.releaseForUser(queuedUser)
      awaitFinished(queuedControl)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(runningControl, queuedControl, prioritizedControl))
      PermitControlledWorkflowState.releaseAll()
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
      QueueControlledTaskState.startedUsers.asScala.toSet shouldBe userUris.toSet

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      cancelAll(controls)
      QueueControlledTaskState.releaseAll()
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

    eventually {
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
    }

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

    eventually {
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
    }

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
    val workflowTask = createLimitedWorkflow("restartHandoff")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val hookEntered = new CountDownLatch(1)
    val allowAcquire = new CountDownLatch(1)
    val runningUser = "urn:test:restart-handoff-running"
    val restartedUser = "urn:test:restart-handoff-restarted"
    val otherQueuedUser = "urn:test:restart-handoff-other"
    val runningControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val control = Activity(RestartQueuedWorkflowExecutor(workflowTask, hookEntered, allowAcquire))
    val otherQueuedControl = Activity(PermitControlledWorkflowExecutor(workflowTask))

    try {
      runningControl.start()(testUserContext(runningUser))
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      control.start()(testUserContext("urn:test:restart-handoff-original"))
      otherQueuedControl.start()(testUserContext(otherQueuedUser))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }

      PermitControlledWorkflowState.releaseForUser(runningUser)
      if(!hookEntered.await(10, TimeUnit.SECONDS)) {
        fail("Timed out waiting for the queued workflow run to reach the queue-to-running handoff.")
      }

      val restartFuture = control.restart()(testUserContext(restartedUser))
      allowAcquire.countDown()

      Await.result(restartFuture, TestTimeout)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
        PermitControlledWorkflowState.startedExecutions.get() shouldBe 2
        PermitControlledWorkflowState.startedUsers.asScala.toSeq.last shouldBe otherQueuedUser
      }
      PermitControlledWorkflowState.releaseForUser(otherQueuedUser)

      eventually {
        PermitControlledWorkflowState.startedExecutions.get() shouldBe 3
        PermitControlledWorkflowState.startedUsers.asScala.toSeq.last shouldBe restartedUser
      }
      PermitControlledWorkflowState.releaseForUser(restartedUser)

      awaitFinished(runningControl)
      awaitFinished(otherQueuedControl)
      awaitFinished(control)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(runningControl, control, otherQueuedControl))
      PermitControlledWorkflowState.releaseAll()
      allowAcquire.countDown()
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
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      val restarts = Future.sequence(restartUsers.toSeq.map(userUri => control.restart()(testUserContext(userUri))))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      firstPermit()
      Await.result(restarts, TestTimeout)
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      val startedUsers = PermitControlledWorkflowState.startedUsers.asScala.toSeq
      startedUsers should have size 1
      restartUsers should contain (startedUsers.head)

      PermitControlledWorkflowState.releaseNext()
      awaitFinished(control)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(control))
      PermitControlledWorkflowState.releaseAll()
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
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      firstPermit()
      if(!hookEntered.await(10, TimeUnit.SECONDS)) {
        fail("Timed out waiting for the queued workflow run to reach the permit-acquisition handoff.")
      }

      control.cancel()(queuedUserContext)
      allowAcquire.countDown()
      awaitFinished(control)

      startedBodies.get() shouldBe 0
      control.status() match {
        case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
        case other => fail(s"Expected the cancelled workflow run to finish in cancelled state, but got: $other")
      }
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(control))
      allowAcquire.countDown()
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

    eventually {
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
    }

    val expectedFailure = TestFailureException("current workflow execution limit lookup failed")
    failLimitLookup.set(Some(expectedFailure))

    eventually {
      waitingThread.isAlive shouldBe false
      waitingFailure.get() shouldBe expectedFailure
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
    }

    try {
      firstPermit()
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      waitingThread.join(TestTimeout.toMillis)
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
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe queuedControls.size
      }

      queuedControls.foreach { case (userUri, control) =>
        control.cancel()(testUserContext(userUri))
      }
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe true
      }

      QueueControlledTaskState.releaseNext()
      awaitFinished(runningControl)
      queuedControls.foreach { case (_, control) => awaitFinished(control) }

      QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe Seq(runningUser)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(runningControl +: queuedControls.map(_._2))
      QueueControlledTaskState.releaseAll()
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
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(2))))
      QueueControlledTaskState.awaitStartedExecutions(2)
      QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe userUris.take(2)

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(1))))
      val thirdControl = startWorkflow(workflowTask, userUris(2))
      try {
        eventually {
          WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
        }

        QueueControlledTaskState.releaseNext()
        awaitFinished(firstControl)
        assertStartedExecutionsStay(2, 800.millis)
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1

        QueueControlledTaskState.releaseNext()
        awaitFinished(secondControl)
        QueueControlledTaskState.awaitStartedExecutions(3)
        QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe userUris

        QueueControlledTaskState.releaseNext()
        awaitFinished(thirdControl)
        eventually {
          WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
        }
      } finally {
        cancelAll(Seq(thirdControl))
      }
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledTaskState.releaseAll()
    }
  }

  it should "remain consistent under a burst of starts, cancels, restarts, and limit updates" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("mixedBurst")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningControl = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queued1 = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queued2 = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queued3 = Activity(PermitControlledWorkflowExecutor(workflowTask))
    val queued4 = Activity(PermitControlledWorkflowExecutor(workflowTask))

    try {
      runningControl.start()(testUserContext("urn:test:mixed-running"))
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      queued1.start()(testUserContext("urn:test:mixed-q1"))
      queued2.start()(testUserContext("urn:test:mixed-q2"))
      queued3.start()(testUserContext("urn:test:mixed-q3"))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 3
      }

      Await.result(queued2.restart()(testUserContext("urn:test:mixed-q2r")), TestTimeout)
      queued3.cancel()(testUserContext("urn:test:mixed-cancel-q3"))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }
      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(2))))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
        PermitControlledWorkflowState.startedExecutions.get() shouldBe 2
        PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:mixed-running", "urn:test:mixed-q1")
      }

      queued4.start()(testUserContext("urn:test:mixed-q4"))
      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(1))))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 2
      }

      PermitControlledWorkflowState.releaseForUser("urn:test:mixed-running")
      awaitFinished(runningControl)
      assertPermitControlledStartsStay(2, 300.millis)

      PermitControlledWorkflowState.releaseForUser("urn:test:mixed-q1")
      awaitFinished(queued1)
      eventually {
        PermitControlledWorkflowState.startedExecutions.get() shouldBe 3
        PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:mixed-running", "urn:test:mixed-q1", "urn:test:mixed-q2r")
      }

      PermitControlledWorkflowState.releaseForUser("urn:test:mixed-q2r")
      awaitFinished(queued2)
      eventually {
        PermitControlledWorkflowState.startedExecutions.get() shouldBe 4
        PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:mixed-running", "urn:test:mixed-q1", "urn:test:mixed-q2r", "urn:test:mixed-q4")
      }

      PermitControlledWorkflowState.releaseForUser("urn:test:mixed-q4")
      awaitFinished(queued3)
      awaitFinished(queued4)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(runningControl, queued1, queued2, queued3, queued4))
      PermitControlledWorkflowState.releaseAll()
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
      PermitControlledWorkflowState.awaitStartedExecutions(2)
      PermitControlledWorkflowState.startedUsers.asScala.toSet shouldBe Set("urn:test:isolation-a1", "urn:test:isolation-b1")
      a2.start()(testUserContext("urn:test:isolation-a2"))
      b2.start()(testUserContext("urn:test:isolation-b2"))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKeyA) shouldBe 1
        WorkflowExecutionLimiter.queuedCount(workflowKeyB) shouldBe 1
      }

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-a1")
      awaitFinished(a1)
      PermitControlledWorkflowState.awaitStartedExecutions(3)
      PermitControlledWorkflowState.startedUsers.asScala.toSet should contain ("urn:test:isolation-a2")
      WorkflowExecutionLimiter.queuedCount(workflowKeyB) shouldBe 1
      assertPermitControlledStartsStay(3, 300.millis)

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-a2")
      awaitFinished(a2)
      assertPermitControlledStartsStay(3, 300.millis)

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-b1")
      awaitFinished(b1)
      PermitControlledWorkflowState.awaitStartedExecutions(4)
      PermitControlledWorkflowState.startedUsers.asScala.toSet should contain ("urn:test:isolation-b2")

      PermitControlledWorkflowState.releaseForUser("urn:test:isolation-b2")
      awaitFinished(b2)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKeyA) shouldBe false
        WorkflowExecutionLimiter.isTracked(workflowKeyB) shouldBe false
      }
    } finally {
      cancelAll(Seq(a1, a2, b1, b2))
      PermitControlledWorkflowState.releaseAll()
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

      limitedControls.foreach(awaitFinished)
      awaitFinished(quickControl)

      QueueControlledTaskState.startedExecutions.get() shouldBe 1
      QuickTaskState.executions.get() shouldBe 1
    } finally {
      cancelAll(limitedControls :+ quickControl)
      QueueControlledTaskState.releaseAll()
    }
  }

  it should "treat repeated queued cancellations as harmless" in {
    PermitControlledWorkflowState.reset()
    val workflowTask = createLimitedWorkflow("doubleCancel")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)
    val runningUser = "urn:test:double-cancel-running"
    val runningControl = startPermitControlledWorkflow(workflowTask, runningUser)
    val queuedUser = "urn:test:double-cancel-queued"
    val queuedControl = startPermitControlledWorkflow(workflowTask, queuedUser)

    try {
      PermitControlledWorkflowState.awaitStartedExecutions(1)
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 1
      }

      queuedControl.cancel()(testUserContext(queuedUser))
      queuedControl.cancel()(testUserContext(queuedUser))
      eventually {
        WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe 0
      }

      PermitControlledWorkflowState.releaseForUser(runningUser)
      awaitFinished(runningControl)
      eventually {
        queuedControl.status() match {
          case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
          case other => fail(s"Expected the repeatedly cancelled queued workflow run to finish cancelled, but got: $other")
        }
      }
      PermitControlledWorkflowState.startedUsers.asScala.toSeq shouldBe Seq(runningUser)
      eventually {
        WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
      }
    } finally {
      cancelAll(Seq(runningControl, queuedControl))
      PermitControlledWorkflowState.releaseAll()
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
      cancelAll(controls)
    }
  }

  private def createLimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
    createBlockingWorkflow(prefix, maxParallelExecutions = Some(1))
  }

  private def createBlockingWorkflow(prefix: String,
                                     maxParallelExecutions: Option[Int]): org.silkframework.workspace.ProjectTask[Workflow] = {
    val project = retrieveOrCreateProject(Identifier(s"${prefix}Project"))
    val blockingTaskId = Identifier(s"${prefix}BlockingTask")
    val workflowId = Identifier(s"${prefix}Workflow")
    project.addTask(blockingTaskId, QueueControlledTask())
    project.addTask(workflowId, Workflow(
      operators = WorkflowOperatorsParameter(Seq(
        WorkflowOperator(
          inputs = Seq.empty,
          task = blockingTaskId,
          outputs = Seq.empty,
          errorOutputs = Seq.empty,
          position = (0, 0),
          nodeId = s"${prefix}Node",
          outputPriority = None,
          configInputs = Seq.empty,
          dependencyInputs = Seq.empty
        )
      )),
      maxParallelExecutions = IntOptionParameter(maxParallelExecutions)
    ))
  }

  private def createUnlimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
    val project = retrieveOrCreateProject(Identifier(s"${prefix}Project"))
    val blockingTaskId = Identifier(s"${prefix}BlockingTask")
    val workflowId = Identifier(s"${prefix}Workflow")
    project.addTask(blockingTaskId, QueueControlledTask())
    project.addTask(workflowId, Workflow(
      operators = WorkflowOperatorsParameter(Seq(
        WorkflowOperator(
          inputs = Seq.empty,
          task = blockingTaskId,
          outputs = Seq.empty,
          errorOutputs = Seq.empty,
          position = (0, 0),
          nodeId = s"${prefix}Node",
          outputPriority = None,
          configInputs = Seq.empty,
          dependencyInputs = Seq.empty
        )
      ))
    ))
  }

  private def createQuickWorkflow(prefix: String,
                                  maxParallelExecutions: Option[Int] = None): org.silkframework.workspace.ProjectTask[Workflow] = {
    val project = retrieveOrCreateProject(Identifier(s"${prefix}Project"))
    val quickTaskId = Identifier(s"${prefix}QuickTask")
    val workflowId = Identifier(s"${prefix}Workflow")
    project.addTask(quickTaskId, QuickTask())
    project.addTask(workflowId, Workflow(
      operators = WorkflowOperatorsParameter(Seq(
        WorkflowOperator(
          inputs = Seq.empty,
          task = quickTaskId,
          outputs = Seq.empty,
          errorOutputs = Seq.empty,
          position = (0, 0),
          nodeId = s"${prefix}Node",
          outputPriority = None,
          configInputs = Seq.empty,
          dependencyInputs = Seq.empty
        )
      )),
      maxParallelExecutions = IntOptionParameter(maxParallelExecutions)
    ))
  }

  private def testUserContext(userUri: String): UserContext = TestWorkflowUserContext(userUri)

  private def startWorkflow(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                            userUri: String): ActivityControl[WorkflowExecutionReport] = {
    val control = Activity(LocalWorkflowExecutor(workflowTask))
    control.start()(testUserContext(userUri))
    control
  }

  private def startPermitControlledWorkflow(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                            userUri: String): ActivityControl[WorkflowExecutionReport] = {
    val control = Activity(PermitControlledWorkflowExecutor(workflowTask))
    control.start()(testUserContext(userUri))
    control
  }

  private def awaitFinished(control: ActivityControl[_]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Await.result(Future(blocking(control.waitUntilFinished())), TestTimeout)
  }

  private def expectQueuedCount(workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey, expected: Int): Unit = {
    eventually {
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe expected
    }
  }

  private def expectUntracked(workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey): Unit = {
    eventually {
      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    }
  }

  private def expectStartedCount(state: StartedExecutionState, expected: Int): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected
    }
  }

  private def expectStartedUsers(state: StartedExecutionState, expected: Seq[String]): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected.size
      state.startedUserSeq shouldBe expected
    }
  }

  private def expectStartedUserSet(state: StartedExecutionState, expected: Set[String]): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected.size
      state.startedUserSet shouldBe expected
    }
  }

  private def expectCancelled(control: ActivityControl[_], failureMessage: String): Unit = {
    eventually {
      control.status() match {
        case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
        case other => fail(s"$failureMessage, but got: $other")
      }
    }
  }

  private def assertStartedExecutionsStay(state: StartedExecutionState, expected: Int, duration: FiniteDuration): Unit = {
    val deadline = System.nanoTime() + duration.toNanos
    while(System.nanoTime() < deadline) {
      state.startedExecutions.get() shouldBe expected
      Thread.sleep(10)
    }
  }

  private def cancelAll(controls: Seq[ActivityControl[_]]): Unit = {
    controls.foreach { control =>
      try {
        control.cancel()(testUserContext("urn:test:cleanup"))
      } catch {
        case _: Throwable =>
      }
    }
  }
}

private case class TestWorkflowUserContext(userUri: String,
                                           override val executionContext: UserExecutionContext = UserExecutionContext()) extends UserContext {
  override def user: Option[User] = Some(new User {
    override def uri: String = userUri
    override def groups: Set[String] = Set.empty
    override def actions: UserActions = UserActions.all
  })

  override def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext = {
    copy(executionContext = userExecutionContext)
  }
}

private case class TestFailureException(message: String) extends RuntimeException(message)

private trait StartedExecutionState {
  val startedExecutions: AtomicInteger
  val startedUsers: ConcurrentLinkedQueue[String]

  final def startedUserSeq: Seq[String] = startedUsers.asScala.toSeq

  final def startedUserSet: Set[String] = startedUsers.asScala.toSet
}

private abstract class ControlledExecutionState(stateLabel: String) extends StartedExecutionState {
  private case class RunningExecution(userUri: String, latch: CountDownLatch)

  override val startedExecutions: AtomicInteger = new AtomicInteger(0)
  override val startedUsers: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()
  private val latches = new LinkedBlockingQueue[RunningExecution]()
  private val latchesByUser = new ConcurrentHashMap[String, RunningExecution]()

  def reset(): Unit = {
    startedExecutions.set(0)
    startedUsers.clear()
    latches.clear()
    latchesByUser.clear()
  }

  def awaitStartedExecutions(expected: Int): Unit = {
    val deadline = System.currentTimeMillis() + 10000
    while(startedExecutions.get() != expected && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
    if(startedExecutions.get() != expected) {
      throw new AssertionError(s"Expected $expected $stateLabel executions, but found ${startedExecutions.get()}.")
    }
  }

  def releaseNext(): Unit = {
    val runningExecution = latches.poll(10, TimeUnit.SECONDS)
    if(runningExecution == null) {
      throw new AssertionError(s"Expected a running $stateLabel execution, but none was available to release.")
    }
    latchesByUser.remove(runningExecution.userUri)
    runningExecution.latch.countDown()
  }

  def releaseForUser(userUri: String): Unit = {
    val runningExecution = latchesByUser.remove(userUri)
    if(runningExecution == null || !latches.remove(runningExecution)) {
      throw new AssertionError(s"Expected a running $stateLabel execution for user '$userUri', but none was available to release.")
    }
    runningExecution.latch.countDown()
  }

  def releaseAll(): Unit = {
    var runningExecution = latches.poll()
    while(runningExecution != null) {
      latchesByUser.remove(runningExecution.userUri)
      runningExecution.latch.countDown()
      runningExecution = latches.poll()
    }
  }

  def registerExecution(userUri: String): CountDownLatch = {
    startedUsers.add(userUri)
    startedExecutions.incrementAndGet()
    val latch = new CountDownLatch(1)
    val runningExecution = RunningExecution(userUri, latch)
    latches.put(runningExecution)
    latchesByUser.put(userUri, runningExecution)
    latch
  }
}

/**
  * Shared test coordination state for [[QueueControlledTask]] executions.
  *
  * Each started task execution registers itself here, which allows tests to observe how many workflow runs have
  * actually entered the blocking task and in which user order. For every started execution a latch is enqueued.
  * Tests then release these latches one by one, or all at once during cleanup, to deterministically unblock the
  * corresponding workflow runs.
  */
private object QueueControlledTaskState extends ControlledExecutionState("queue-controlled task")

/**
  * Shared coordination state for permit-only workflow executor tests.
  *
  * These tests do not execute the workflow body. They only acquire a workflow execution permit, record the started
  * user, and then block on a per-run latch. This keeps limiter and lifecycle tests deterministic without depending
  * on actual workflow operator execution.
  */
private object PermitControlledWorkflowState extends ControlledExecutionState("permit-controlled workflow")

/**
  * Test task that deliberately blocks workflow progress until the test releases it.
  *
  * This is used to keep workflow executions in a controlled running state, so tests can verify queueing,
  * cancellation, and parallel-start behavior around the workflow execution limiter.
  */
private case class QueueControlledTask() extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

/**
  * Executor for [[QueueControlledTask]].
  *
  * Each execution registers itself in [[QueueControlledTaskState]], then waits on a per-execution latch.
  * The test can observe how many executions have actually started and release them one by one via that shared state.
  */
private case class QueueControlledTaskExecutor() extends Executor[QueueControlledTask, ExecutionType] {
  override def execute(task: Task[QueueControlledTask],
                       inputs: Seq[ExecutionType#DataType],
                       output: ExecutorOutput,
                       execution: ExecutionType,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[ExecutionType#DataType] = {
    val userUri = pluginContext.user.user.map(_.uri).getOrElse("anonymous")
    val latch = QueueControlledTaskState.registerExecution(userUri)
    if(!latch.await(30, TimeUnit.SECONDS)) {
      throw new AssertionError("QueueControlledTask was not released within the test timeout.")
    }
    None
  }
}

private object QuickTaskState {
  val executions: AtomicInteger = new AtomicInteger(0)
  private val startTimes = new ConcurrentLinkedQueue[java.lang.Long]()

  def reset(): Unit = {
    executions.set(0)
    startTimes.clear()
  }

  def registerExecutionStart(): Unit = {
    executions.incrementAndGet()
    startTimes.add(System.nanoTime())
  }

  def executionStartTimes(): Seq[Long] = {
    startTimes.asScala.iterator.map(_.longValue()).toSeq
  }
}

private case class QuickTask() extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

private case class QuickTaskExecutor() extends Executor[QuickTask, ExecutionType] {
  override def execute(task: Task[QuickTask],
                       inputs: Seq[ExecutionType#DataType],
                       output: ExecutorOutput,
                       execution: ExecutionType,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[ExecutionType#DataType] = {
    QuickTaskState.registerExecutionStart()
    None
  }
}

private case class TestWorkflowPermitExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow]) extends WorkflowExecutor[LocalExecution] {
  override protected def replaceDataSources: Map[String, Dataset] = Map.empty
  override protected def replaceSinks: Map[String, Dataset] = Map.empty
  override protected def executionContext: LocalExecution = LocalExecution()

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override def run(context: ActivityContext[WorkflowExecutionReport])(implicit userContext: UserContext): Unit = ()

  def acquirePermitForTests(context: ActivityContext[WorkflowExecutionReport]): Option[() => Unit] = {
    acquireExecutionPermit(context).map(handle => () => handle.release())
  }

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: org.silkframework.config.TaskSpec])
                                                (process: Option[org.silkframework.execution.EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    throw new UnsupportedOperationException("workflowNodeEntities is not needed for permit acquisition tests.")
  }
}

private abstract class ControlledPermitWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                                        timeoutMessage: String) extends WorkflowExecutor[LocalExecution] {
  override protected def replaceDataSources: Map[String, Dataset] = Map.empty
  override protected def replaceSinks: Map[String, Dataset] = Map.empty
  override protected def executionContext: LocalExecution = LocalExecution()

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override def run(context: ActivityContext[WorkflowExecutionReport])(implicit userContext: UserContext): Unit = {
    acquireExecutionPermit(context) match {
      case Some(permitHandle) =>
        val userUri = userContext.user.map(_.uri).getOrElse("anonymous")
        val latch = PermitControlledWorkflowState.registerExecution(userUri)
        try {
          if(!latch.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError(timeoutMessage)
          }
        } finally {
          permitHandle.release()
        }
      case None =>
    }
  }

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: org.silkframework.config.TaskSpec])
                                                (process: Option[org.silkframework.execution.EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    throw new UnsupportedOperationException("workflowNodeEntities is not needed for controlled permit workflow tests.")
  }
}

private case class PermitControlledWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow])
  extends ControlledPermitWorkflowExecutor(workflowTask, "Permit-controlled workflow execution was not released within the test timeout.")

private case class RestartQueuedWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                                 hookEntered: CountDownLatch,
                                                 allowAcquire: CountDownLatch)
  extends ControlledPermitWorkflowExecutor(workflowTask, "Restart-handoff workflow execution was not released within the test timeout.") {
  override protected def beforeQueuedPermitAcquire(context: ActivityContext[WorkflowExecutionReport],
                                                   workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey,
                                                   queuedToken: WorkflowExecutionLimiter.QueueToken): Unit = {
    val userUri = context.startedBy.user.map(_.uri).getOrElse("")
    if(userUri == "urn:test:restart-handoff-original") {
      hookEntered.countDown()
      while(allowAcquire.getCount > 0) {
        try {
          Thread.sleep(5L)
        } catch {
          case _: InterruptedException =>
        }
      }
    }
  }
}

private case class CancelRaceWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                              hookEntered: CountDownLatch,
                                              allowAcquire: CountDownLatch,
                                              startedBodies: AtomicInteger) extends WorkflowExecutor[LocalExecution] {
  override protected def replaceDataSources: Map[String, Dataset] = Map.empty
  override protected def replaceSinks: Map[String, Dataset] = Map.empty
  override protected def executionContext: LocalExecution = LocalExecution()

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override protected def beforeQueuedPermitAcquire(context: ActivityContext[WorkflowExecutionReport],
                                                   workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey,
                                                   queuedToken: WorkflowExecutionLimiter.QueueToken): Unit = {
    hookEntered.countDown()
    while(allowAcquire.getCount > 0) {
      try {
        Thread.sleep(5L)
      } catch {
        case _: InterruptedException =>
      }
    }
  }

  override def run(context: ActivityContext[WorkflowExecutionReport])(implicit userContext: UserContext): Unit = {
    acquireExecutionPermit(context) match {
      case Some(permitHandle) =>
        try {
          startedBodies.incrementAndGet()
        } finally {
          permitHandle.release()
        }
      case None =>
    }
  }

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: org.silkframework.config.TaskSpec])
                                                (process: Option[org.silkframework.execution.EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    throw new UnsupportedOperationException("workflowNodeEntities is not needed for cancel-race tests.")
  }
}

private case class FailingLimitLookupWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                                      failureToThrow: AtomicReference[Option[TestFailureException]]) extends WorkflowExecutor[LocalExecution] {
  override protected def replaceDataSources: Map[String, Dataset] = Map.empty
  override protected def replaceSinks: Map[String, Dataset] = Map.empty
  override protected def executionContext: LocalExecution = LocalExecution()

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override protected def currentWorkflowExecutionLimit(context: ActivityContext[WorkflowExecutionReport]): Option[Int] = {
    failureToThrow.get() match {
      case Some(ex) => throw ex
      case None => super.currentWorkflowExecutionLimit(context)
    }
  }

  override def run(context: ActivityContext[WorkflowExecutionReport])(implicit userContext: UserContext): Unit = ()

  def acquirePermitForTests(context: ActivityContext[WorkflowExecutionReport]): Option[() => Unit] = {
    acquireExecutionPermit(context).map(handle => () => handle.release())
  }

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: org.silkframework.config.TaskSpec])
                                                (process: Option[org.silkframework.execution.EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    throw new UnsupportedOperationException("workflowNodeEntities is not needed for limit-lookup failure tests.")
  }
}
