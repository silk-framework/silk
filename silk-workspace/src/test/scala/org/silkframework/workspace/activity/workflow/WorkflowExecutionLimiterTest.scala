package org.silkframework.workspace.activity.workflow

import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.silkframework.config.Task
import org.silkframework.runtime.activity._
import org.silkframework.runtime.activity.Status.{Finished, Waiting}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.types.IntOptionParameter
import org.silkframework.runtime.users.{User, UserActions}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{ActivityExecutionLimiter, ActivityLimiterKey, QueuedActivityControl, TaskActivityFactory}

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters._

class WorkflowExecutionLimiterTest extends AnyFlatSpec with Matchers with Eventually with TestWorkspaceProviderTestTrait with TestUserContextTrait {
  private val TestTimeout: FiniteDuration = 25.seconds
  private val workflowLimit = WorkflowExecutionActivityLimit()
  private val guardedFactory = GuardedWorkflowExecutionFactory()

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(TestTimeout.toSeconds, Seconds)))

  behavior of "WorkflowExecutionLimiter"

  it should "grant queued workflow runs in FIFO order" in {
    val limiterKey = ActivityLimiterKey(Some(Identifier("fifoProject")), Some(Identifier("fifoWorkflow")), "workflow-execution")
    val initialPermit = ActivityExecutionLimiter.requestSlot(limiterKey, Some(1)) match {
      case ActivityExecutionLimiter.Acquired(permit) => permit
      case other => fail(s"Expected the first workflow run to acquire a permit immediately, but got: $other")
    }
    val queuedTokens = (1 to 3).map { _ =>
      ActivityExecutionLimiter.requestSlot(limiterKey, Some(1)) match {
        case ActivityExecutionLimiter.Queued(token) => token
        case other => fail(s"Expected queued workflow run, but got: $other")
      }
    }

    try {
      ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(1), Some(1)) shouldBe None
      ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(2), Some(1)) shouldBe None

      initialPermit.release()

      val secondPermit = ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens.head, Some(1)).getOrElse {
        fail("Expected the first queued workflow run to acquire the freed permit.")
      }
      ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(1), Some(1)) shouldBe None
      ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(2), Some(1)) shouldBe None

      secondPermit.release()

      val thirdPermit = ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(1), Some(1)).getOrElse {
        fail("Expected the second queued workflow run to acquire the next freed permit.")
      }
      ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(2), Some(1)) shouldBe None

      thirdPermit.release()

      val fourthPermit = ActivityExecutionLimiter.acquireQueued(limiterKey, queuedTokens(2), Some(1)).getOrElse {
        fail("Expected the third queued workflow run to acquire the final freed permit.")
      }
      fourthPermit.release()

      ActivityExecutionLimiter.isTracked(limiterKey) shouldBe false
    } finally {
      queuedTokens.foreach(token => ActivityExecutionLimiter.cancelQueued(limiterKey, token))
    }
  }

  it should "remove waiting workflow runs from the queue when they are cancelled" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("cancel")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "workflow-execution")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val secondControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val secondUserContext = testUserContext("urn:test:queued")

    try {
      firstControl.start()(testUserContext("urn:test:running"))
      QueueControlledActivityState.awaitStartedExecutions(1)
      eventually {
        firstControl.status() shouldBe a[Status.Running]
      }

      secondControl.start()(secondUserContext)
      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 1
      }

      secondControl.cancel()(secondUserContext)
      QueueControlledActivityState.releaseNext()

      awaitFinished(firstControl)
      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 0
        secondControl.status() match {
          case Finished(_, _, cancelled, _) => cancelled shouldBe true
          case other => fail(s"Expected queued workflow run to finish in cancelled state, but got: $other")
        }
      }

      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:running")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "re-evaluate the workflow limit while waiting" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("reconfigure")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val secondControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))

    try {
      firstControl.start()(testUserContext("urn:test:first"))
      secondControl.start()(testUserContext("urn:test:second"))

      QueueControlledActivityState.awaitStartedExecutions(1)
      eventually {
        secondControl.status() shouldBe a[Waiting]
      }

      workflowTask.project.updateTask(workflowTask.id, workflowTask.data.copy(maxParallelExecutions = IntOptionParameter(Some(2))))
      QueueControlledActivityState.awaitStartedExecutions(2)

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:first", "urn:test:second")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "bypass the limiter completely for unlimited workflows" in {
    QueueControlledActivityState.reset()
    val workflowTask = createUnlimitedWorkflow("unlimited")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "workflow-execution")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val secondControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))

    try {
      firstControl.start()(testUserContext("urn:test:unlimited-first"))
      secondControl.start()(testUserContext("urn:test:unlimited-second"))

      QueueControlledActivityState.awaitStartedExecutions(2)
      ActivityExecutionLimiter.isTracked(limiterKey) shouldBe false

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "not block the activity thread pool while workflow runs are waiting for a slot" in {
    QueueControlledActivityState.reset()
    QuickActivityState.reset()
    val workflowTask = createLimitedWorkflow("pool")
    val parallelism = ActivityExecution.forkJoinPool.getParallelism

    val limitedControls =
      (1 to parallelism).map { _ =>
        guardedControl(workflowTask, Activity(QueueControlledActivity()))
      }

    limitedControls.zipWithIndex.foreach { case (control, index) =>
      control.start()(testUserContext(s"urn:test:pool-${index + 1}"))
    }

    QueueControlledActivityState.awaitStartedExecutions(1)

    val quickControl = Activity(QuickActivity())

    try {
      quickControl.start()(testUserContext("urn:test:quick"))

      eventually {
        QuickActivityState.executions.get() shouldBe 1
      }

      limitedControls.tail.foreach(_.cancel()(testUserContext("urn:test:cancel")))
      QueueControlledActivityState.releaseNext()

      limitedControls.foreach(awaitFinished)
      awaitFinished(quickControl)

      QueueControlledActivityState.startedExecutions.get() shouldBe 1
      QuickActivityState.executions.get() shouldBe 1
    } finally {
      cancelAll(limitedControls :+ quickControl)
      QueueControlledActivityState.releaseAll()
    }
  }

  private def createLimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
    createWorkflow(prefix, maxParallelExecutions = Some(1))
  }

  private def createUnlimitedWorkflow(prefix: String): org.silkframework.workspace.ProjectTask[Workflow] = {
    createWorkflow(prefix, maxParallelExecutions = None)
  }

  private def createWorkflow(prefix: String, maxParallelExecutions: Option[Int]): org.silkframework.workspace.ProjectTask[Workflow] = {
    val project = retrieveOrCreateProject(Identifier(s"${prefix}Project"))
    val workflowId = Identifier(s"${prefix}Workflow")
    project.addTask(workflowId, Workflow(
      operators = WorkflowOperatorsParameter(Seq.empty),
      maxParallelExecutions = IntOptionParameter(maxParallelExecutions)
    ))
  }

  private def guardedControl(workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                             delegate: ActivityControl[Unit]): QueuedActivityControl[Unit] = {
    new QueuedActivityControl(
      delegate = delegate,
      task = Some(workflowTask),
      factory = guardedFactory,
      limit = workflowLimit
    )
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

private object QueueControlledActivityState {
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
      throw new AssertionError("Expected a running queue-controlled activity, but none was available to release.")
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

private case class QueueControlledActivity() extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    val userUri = userContext.user.map(_.uri).getOrElse("anonymous")
    val latch = QueueControlledActivityState.registerExecution(userUri)
    if(!latch.await(30, TimeUnit.SECONDS)) {
      throw new AssertionError("QueueControlledActivity was not released within the test timeout.")
    }
  }
}

private object QuickActivityState {
  val executions: AtomicInteger = new AtomicInteger(0)

  def reset(): Unit = {
    executions.set(0)
  }
}

private case class QuickActivity() extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    QuickActivityState.executions.incrementAndGet()
  }
}

private trait GuardedWorkflowExecution extends HasValue {
  override type ValueType = Unit
}

@Plugin(
  id = "ExecuteWorkflowWithPayload",
  label = "Execute with payload",
  categories = Array("WorkflowExecution")
)
private case class GuardedWorkflowExecutionFactory() extends TaskActivityFactory[Workflow, GuardedWorkflowExecution] {
  override def isSingleton: Boolean = false

  override def apply(task: ProjectTask[Workflow]): Activity[Unit] = QueueControlledActivity()
}
