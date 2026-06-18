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
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue, TimeUnit}
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
    QueueControlledTaskState.reset()
    val workflowTask = createLimitedWorkflow("cancel")

    val firstControl = Activity(LocalWorkflowExecutor(workflowTask))
    firstControl.start()(testUserContext("urn:test:running"))

    val queuedControl = Activity(LocalWorkflowExecutor(workflowTask))
    val queuedUserContext = testUserContext("urn:test:queued")
    queuedControl.start()(queuedUserContext)

    try {
      QueueControlledTaskState.awaitStartedExecutions(1)
      queuedControl.cancel()(queuedUserContext)
      QueueControlledTaskState.releaseNext()

      awaitFinished(firstControl)
      awaitFinished(queuedControl)

      QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:running")
      queuedControl.status() match {
        case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
        case other => fail(s"Expected queued workflow run to finish in cancelled state, but got: $other")
      }
    } finally {
      cancelAll(Seq(firstControl, queuedControl))
      QueueControlledTaskState.releaseAll()
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

      QueueControlledTaskState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:first", "urn:test:second")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledTaskState.releaseAll()
    }
  }

  it should "bypass the limiter completely for unlimited workflows" in {
    QueueControlledTaskState.reset()
    val workflowTask = createUnlimitedWorkflow("unlimited")
    val workflowKey = WorkflowExecutionLimiter.WorkflowExecutionKey(workflowTask.project.id, workflowTask.id)

    val firstControl = Activity(LocalWorkflowExecutor(workflowTask))
    firstControl.start()(testUserContext("urn:test:unlimited-first"))

    val secondControl = Activity(LocalWorkflowExecutor(workflowTask))
    secondControl.start()(testUserContext("urn:test:unlimited-second"))

    try {
      QueueControlledTaskState.awaitStartedExecutions(2)
      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false

      QueueControlledTaskState.releaseNext()
      QueueControlledTaskState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    } finally {
      cancelAll(Seq(firstControl, secondControl))
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

  private def createLimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
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
      maxParallelExecutions = IntOptionParameter(Some(1))
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

  private def createQuickWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
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
      ))
    ))
  }

  private def testUserContext(userUri: String): UserContext = TestWorkflowUserContext(userUri)

  private def awaitFinished(control: ActivityControl[_]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Await.result(Future(blocking(control.waitUntilFinished())), TestTimeout)
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

/**
  * Shared test coordination state for [[QueueControlledTask]] executions.
  *
  * Each started task execution registers itself here, which allows tests to observe how many workflow runs have
  * actually entered the blocking task and in which user order. For every started execution a latch is enqueued.
  * Tests then release these latches one by one, or all at once during cleanup, to deterministically unblock the
  * corresponding workflow runs.
  */
private object QueueControlledTaskState {
  val startedExecutions: AtomicInteger = new AtomicInteger(0)
  val startedUsers: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()
  private val latches = new LinkedBlockingQueue[CountDownLatch]()

  def reset(): Unit = {
    startedExecutions.set(0)
    startedUsers.clear()
    latches.clear()
  }

  def awaitStartedExecutions(expected: Int): Unit = {
    val deadline = System.currentTimeMillis() + 10000
    while(startedExecutions.get() != expected && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
    if(startedExecutions.get() != expected) {
      throw new AssertionError(s"Expected $expected started executions, but found ${startedExecutions.get()}.")
    }
  }

  def releaseNext(): Unit = {
    val latch = latches.poll(10, TimeUnit.SECONDS)
    if(latch == null) {
      throw new AssertionError("Expected a running queue-controlled task, but none was available to release.")
    }
    latch.countDown()
  }

  def releaseAll(): Unit = {
    var latch = latches.poll()
    while(latch != null) {
      latch.countDown()
      latch = latches.poll()
    }
  }

  def registerExecution(userUri: String): CountDownLatch = {
    startedUsers.add(userUri)
    startedExecutions.incrementAndGet()
    val latch = new CountDownLatch(1)
    latches.put(latch)
    latch
  }
}

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

  def reset(): Unit = {
    executions.set(0)
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
    QuickTaskState.executions.incrementAndGet()
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
