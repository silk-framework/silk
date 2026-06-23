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
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.plugin.types.IntOptionParameter
import org.silkframework.runtime.users.{User, UserActions}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TestWorkspaceProviderTestTrait

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters._

abstract class WorkflowExecutionLimiterTestSupport
  extends AnyFlatSpec
    with Matchers
    with Eventually
    with TestWorkspaceProviderTestTrait
    with BeforeAndAfterAll
    with TestUserContextTrait {

  protected val TestTimeout: FiniteDuration = 25.seconds

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

  protected final def createLimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
    createBlockingWorkflow(prefix, maxParallelExecutions = Some(1))
  }

  protected final def createBlockingWorkflow(prefix: String,
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

  protected final def createUnlimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
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

  protected final def createQuickWorkflow(prefix: String,
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

  protected final def testUserContext(userUri: String): UserContext = TestWorkflowUserContext(userUri)

  protected final def startWorkflow(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                    userUri: String): ActivityControl[WorkflowExecutionReport] = {
    val control = Activity(LocalWorkflowExecutor(workflowTask))
    control.start()(testUserContext(userUri))
    control
  }

  protected final def startPermitControlledWorkflow(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                                    userUri: String): ActivityControl[WorkflowExecutionReport] = {
    val control = Activity(PermitControlledWorkflowExecutor(workflowTask))
    control.start()(testUserContext(userUri))
    control
  }

  protected final def awaitFinished(control: ActivityControl[_]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Await.result(Future(blocking(control.waitUntilFinished())), TestTimeout)
  }

  protected final def expectQueuedCount(workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey, expected: Int): Unit = {
    eventually {
      WorkflowExecutionLimiter.queuedCount(workflowKey) shouldBe expected
    }
  }

  protected final def expectUntracked(workflowKey: WorkflowExecutionLimiter.WorkflowExecutionKey): Unit = {
    eventually {
      WorkflowExecutionLimiter.isTracked(workflowKey) shouldBe false
    }
  }

  protected final def expectStartedCount(state: StartedExecutionState, expected: Int): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected
    }
  }

  protected final def expectStartedUsers(state: StartedExecutionState, expected: Seq[String]): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected.size
      state.startedUserSeq shouldBe expected
    }
  }

  protected final def expectStartedUserSet(state: StartedExecutionState, expected: Set[String]): Unit = {
    eventually {
      state.startedExecutions.get() shouldBe expected.size
      state.startedUserSet shouldBe expected
    }
  }

  protected final def expectCancelled(control: ActivityControl[_], failureMessage: String): Unit = {
    eventually {
      control.status() match {
        case Status.Finished(_, _, cancelled, _) => cancelled shouldBe true
        case other => fail(s"$failureMessage, but got: $other")
      }
    }
  }

  protected final def assertStartedExecutionsStay(state: StartedExecutionState, expected: Int, duration: FiniteDuration): Unit = {
    val deadline = System.nanoTime() + duration.toNanos
    while(System.nanoTime() < deadline) {
      state.startedExecutions.get() shouldBe expected
      Thread.sleep(10)
    }
  }

  protected final def cancelAll(controls: Seq[ActivityControl[_]]): Unit = {
    controls.foreach { control =>
      try {
        control.cancel()(testUserContext("urn:test:cleanup"))
      } catch {
        case _: Throwable =>
      }
    }
  }
}

case class TestWorkflowUserContext(userUri: String,
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

case class TestFailureException(message: String) extends RuntimeException(message)

trait StartedExecutionState {
  val startedExecutions: AtomicInteger
  val startedUsers: ConcurrentLinkedQueue[String]

  final def startedUserSeq: Seq[String] = startedUsers.asScala.toSeq

  final def startedUserSet: Set[String] = startedUsers.asScala.toSet
}

abstract class ControlledExecutionState(stateLabel: String) extends StartedExecutionState {
  private case class RunningExecution(userUri: String, latch: CountDownLatch)

  // Counts how many executions have entered the controlled section at least once. This is cumulative test history,
  // not the number of executions that are currently running.
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
    // Use this only when the test intentionally relies on FIFO release order. Most limiter tests should prefer
    // releaseForUser(...) so they don't depend on incidental latch queue ordering.
    val runningExecution = latches.poll(10, TimeUnit.SECONDS)
    if(runningExecution == null) {
      throw new AssertionError(s"Expected a running $stateLabel execution, but none was available to release.")
    }
    latchesByUser.remove(runningExecution.userUri)
    runningExecution.latch.countDown()
  }

  def releaseForUser(userUri: String): Unit = {
    // Releasing by user keeps the test tied to the intended workflow handoff rather than to the internal latch queue.
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
object QueueControlledTaskState extends ControlledExecutionState("queue-controlled task")

/**
  * Shared coordination state for permit-only workflow executor tests.
  *
  * These tests do not execute the workflow body. They only acquire a workflow execution permit, record the started
  * user, and then block on a per-run latch. This keeps limiter and lifecycle tests deterministic without depending
  * on actual workflow operator execution.
  */
object PermitControlledWorkflowState extends ControlledExecutionState("permit-controlled workflow")

/**
  * Test task that deliberately blocks workflow progress until the test releases it.
  *
  * This is used to keep workflow executions in a controlled running state, so tests can verify queueing,
  * cancellation, and parallel-start behavior around the workflow execution limiter.
  */
case class QueueControlledTask() extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

/**
  * Executor for [[QueueControlledTask]].
  *
  * Each execution registers itself in [[QueueControlledTaskState]], then waits on a per-execution latch.
  * The test can observe how many executions have actually started and release them one by one via that shared state.
  */
case class QueueControlledTaskExecutor() extends Executor[QueueControlledTask, ExecutionType] {
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

object QuickTaskState {
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

case class QuickTask() extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

case class QuickTaskExecutor() extends Executor[QuickTask, ExecutionType] {
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

case class TestWorkflowPermitExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow]) extends WorkflowExecutor[LocalExecution] {
  override protected def replaceDataSources: Map[String, Dataset] = Map.empty
  override protected def replaceSinks: Map[String, Dataset] = Map.empty
  override protected def executionContext: LocalExecution = LocalExecution()

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override def run(context: ActivityContext[WorkflowExecutionReport])(implicit userContext: UserContext): Unit = ()

  def acquirePermitForTests(context: ActivityContext[WorkflowExecutionReport]): Option[() => Unit] = {
    // Return the permit release as a callback so the test can hold and release the slot without running a workflow body.
    acquireExecutionPermit(context).map(handle => () => handle.release())
  }

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: org.silkframework.config.TaskSpec])
                                                (process: Option[org.silkframework.execution.EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    throw new UnsupportedOperationException("workflowNodeEntities is not needed for permit acquisition tests.")
  }
}

abstract class ControlledPermitWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
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

case class PermitControlledWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow])
  extends ControlledPermitWorkflowExecutor(workflowTask, "Permit-controlled workflow execution was not released within the test timeout.")

case class RestartQueuedWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
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

case class CancelRaceWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
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

case class FailingLimitLookupWorkflowExecutor(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
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
