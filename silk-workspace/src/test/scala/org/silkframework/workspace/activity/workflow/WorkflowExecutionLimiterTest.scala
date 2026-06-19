package org.silkframework.workspace.activity.workflow

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.silkframework.config.Task
import org.silkframework.runtime.activity._
import org.silkframework.runtime.activity.Status.{Finished, Waiting}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.types.IntOptionParameter
import org.silkframework.runtime.users.{User, UserActions}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{ActivityExecutionLimiter, ActivityLimit, ActivityLimiterKey, QueuedActivityControl, TaskActivityFactory, WorkspaceActivity, WorkspaceActivityFactory}

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, LinkedBlockingQueue, TimeUnit}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters._

class WorkflowExecutionLimiterTest extends AnyFlatSpec with Matchers with Eventually with TestWorkspaceProviderTestTrait with TestUserContextTrait with BeforeAndAfterAll {
  private val TestTimeout: FiniteDuration = 25.seconds
  private val workflowLimit = WorkflowExecutionActivityLimit()
  private val guardedFactory = GuardedWorkflowExecutionFactory()

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(TestTimeout.toSeconds, Seconds)))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[GuardedWorkflowExecutionFactory])
    PluginRegistry.registerPlugin(classOf[UnguardedWorkflowExecutionFactory])
    PluginRegistry.registerPlugin(classOf[SelectionWorkflowExecutionFactory])
    PluginRegistry.registerPlugin(classOf[NoLimitSelectionWorkflowExecutionActivityLimit])
    PluginRegistry.registerPlugin(classOf[SelectionWorkflowExecutionActivityLimit])
  }

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
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:running"))
      QueueControlledActivityState.awaitStartedExecutions(1)
      eventually {
        firstControl.status() shouldBe a[Status.Running]
      }

      startAndAwaitQueuedOrRunning(secondControl, secondUserContext)
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
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:first"))
      startAndAwaitQueuedOrRunning(secondControl, testUserContext("urn:test:second"))

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
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:unlimited-first"))
      startAndAwaitQueuedOrRunning(secondControl, testUserContext("urn:test:unlimited-second"))

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

  it should "install the wrapper end-to-end for guarded workflow-starting activities" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("guardedIntegration")
    val activity = guardedActivity(workflowTask)
    val instanceId = activity.start()(testUserContext("urn:test:guarded-integration"))
    val control = activity.instance(instanceId)

    try {
      control shouldBe a[QueuedActivityControl[_]]
      QueueControlledActivityState.releaseNext()
      awaitFinished(control)
    } finally {
      cancelAll(Seq(control))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "not install the wrapper for unguarded workflow activities" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("unguardedIntegration")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "workflow-execution")
    val activity = unguardedActivity(workflowTask)

    activity.control should not be a[QueuedActivityControl[_]]

    val firstId = activity.start()(testUserContext("urn:test:unguarded-first"))
    val secondId = activity.start()(testUserContext("urn:test:unguarded-second"))
    val firstControl = activity.instance(firstId)
    val secondControl = activity.instance(secondId)

    try {
      QueueControlledActivityState.awaitStartedExecutions(2)
      ActivityExecutionLimiter.isTracked(limiterKey) shouldBe false

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:unguarded-first", "urn:test:unguarded-second")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "start prioritized queued runs immediately" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("prioritized")
    val activity = guardedActivity(workflowTask)

    val firstId = activity.start()(testUserContext("urn:test:running"))
    QueueControlledActivityState.awaitStartedExecutions(1)
    val secondId = startAndAwaitQueuedOrRunning(activity, testUserContext("urn:test:queued"))
    val firstControl = activity.instance(firstId)
    val secondControl = activity.instance(secondId)

    try {
      eventually {
        secondControl.status() shouldBe a[Waiting]
      }

      secondControl.startPrioritized()(testUserContext("urn:test:prioritized"))

      QueueControlledActivityState.awaitStartedExecutions(2)
      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:running", "urn:test:queued")

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "block in startBlocking while queued until the wrapped run actually finishes" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("startBlocking")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val secondControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))

    try {
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:running"))
      QueueControlledActivityState.awaitStartedExecutions(1)

      val blockingFuture = Future(blocking(secondControl.startBlocking()(testUserContext("urn:test:blocking"))))

      eventually {
        secondControl.status() shouldBe a[Waiting]
        blockingFuture.isCompleted shouldBe false
      }

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.awaitStartedExecutions(2)
      blockingFuture.isCompleted shouldBe false

      QueueControlledActivityState.releaseNext()
      Await.result(blockingFuture, TestTimeout)
      awaitFinished(firstControl)
      awaitFinished(secondControl)
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "block in startBlockingAndGetValue while queued and preload the delegate value" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("startBlockingValue")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val valueActivity = ValueProducingActivity("done")
    val secondControl = guardedControl(workflowTask, Activity(valueActivity))

    try {
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:running"))
      QueueControlledActivityState.awaitStartedExecutions(1)

      val blockingFuture = Future(blocking(secondControl.startBlockingAndGetValue(Some("prefilled"))(testUserContext("urn:test:blocking-value"))))

      eventually {
        secondControl.status() shouldBe a[Waiting]
        secondControl.value() shouldBe "prefilled"
        blockingFuture.isCompleted shouldBe false
      }

      QueueControlledActivityState.releaseNext()
      valueActivity.awaitStarted()
      blockingFuture.isCompleted shouldBe false

      valueActivity.release()
      Await.result(blockingFuture, TestTimeout) shouldBe "done"
      awaitFinished(firstControl)
      awaitFinished(secondControl)
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
      valueActivity.release()
    }
  }

  it should "restart queued runs through the wrapper lifecycle" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("restart")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "workflow-execution")
    val activity = guardedActivity(workflowTask)

    val firstId = startAndAwaitQueuedOrRunning(activity, testUserContext("urn:test:running"))
    QueueControlledActivityState.awaitStartedExecutions(1)
    val secondId = startAndAwaitQueuedOrRunning(activity, testUserContext("urn:test:queued-initial"))
    val firstControl = activity.instance(firstId)
    val secondControl = activity.instance(secondId)

    try {
      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 1
      }

      secondControl.restart()(testUserContext("urn:test:queued-restarted"))

      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 1
      }

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.awaitStartedExecutions(2)
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)

      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:running", "urn:test:queued-restarted")
    } finally {
      cancelAll(Seq(firstControl, secondControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "forward cancel to the delegate after the wrapped run has started" in {
    val workflowTask = createUnlimitedWorkflow("cancelRunning")
    val activity = InterruptibleActivity()
    val control = guardedControl(workflowTask, Activity(activity))

    try {
      control.start()(testUserContext("urn:test:running"))
      activity.awaitStarted()

      control.cancel()(testUserContext("urn:test:cancel"))

      eventually {
        activity.cancelInvocations.get() shouldBe 1
        control.status() match {
          case Finished(_, _, cancelled, _) => cancelled shouldBe true
          case other => fail(s"Expected cancelled status after delegate cancellation, but got: $other")
        }
      }
    } finally {
      cancelAll(Seq(control))
      activity.release()
    }
  }

  it should "forward prioritize to the delegate after the wrapped run has started" in {
    val workflowTask = createUnlimitedWorkflow("prioritizeRunning")
    val delegate = new PrioritizationProbeControl()
    val control = guardedControl(workflowTask, delegate)

    try {
      control.start()(testUserContext("urn:test:running"))
      delegate.awaitStarted()

      control.startPrioritized()(testUserContext("urn:test:prioritized"))

      eventually {
        delegate.prioritizedCalls.get() shouldBe 1
      }
    } finally {
      delegate.finish()
      control.waitUntilFinished()
    }
  }

  it should "surface delegate failures after queue waiting" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("failure")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "workflow-execution")
    val testFailure = TestFailureException("delegate failed")
    val firstControl = guardedControl(workflowTask, Activity(QueueControlledActivity()))
    val failingControl = guardedControl(workflowTask, Activity(FailingActivity(testFailure)))

    try {
      startAndAwaitQueuedOrRunning(firstControl, testUserContext("urn:test:running"))
      QueueControlledActivityState.awaitStartedExecutions(1)
      startAndAwaitQueuedOrRunning(failingControl, testUserContext("urn:test:failing"))

      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 1
      }

      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)

      val ex = the[TestFailureException] thrownBy {
        failingControl.waitUntilFinished()
      }
      ex shouldBe testFailure
      eventually {
        ActivityExecutionLimiter.isTracked(limiterKey) shouldBe false
      }
      failingControl.status() match {
        case Finished(success, _, cancelled, Some(exception)) =>
          success shouldBe false
          cancelled shouldBe false
          exception shouldBe testFailure
        case other =>
          fail(s"Expected failed status after delegate exception, but got: $other")
      }
    } finally {
      cancelAll(Seq(firstControl, failingControl))
      QueueControlledActivityState.releaseAll()
    }
  }

  it should "use the first limiter plugin that provides a defined limit" in {
    QueueControlledActivityState.reset()
    val workflowTask = createLimitedWorkflow("selection")
    val limiterKey = ActivityLimiterKey(Some(workflowTask.project.id), Some(workflowTask.id), "selection-workflow-execution")
    val activity = selectionActivity(workflowTask)

    val firstId = startAndAwaitQueuedOrRunning(activity, testUserContext("urn:test:first"))
    val secondId = startAndAwaitQueuedOrRunning(activity, testUserContext("urn:test:second"))
    val firstControl = activity.instance(firstId)
    val secondControl = activity.instance(secondId)

    try {
      QueueControlledActivityState.awaitStartedExecutions(1)
      eventually {
        ActivityExecutionLimiter.queuedCount(limiterKey) shouldBe 1
      }

      QueueControlledActivityState.releaseNext()
      QueueControlledActivityState.awaitStartedExecutions(2)
      QueueControlledActivityState.releaseNext()
      awaitFinished(firstControl)
      awaitFinished(secondControl)
      QueueControlledActivityState.startedUsers.asScala.toSeq shouldBe Seq("urn:test:first", "urn:test:second")
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
      startAndAwaitQueuedOrRunning(control, testUserContext(s"urn:test:pool-${index + 1}"))
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

  private def guardedActivity(workflowTask: org.silkframework.workspace.ProjectTask[Workflow]) = {
    workflowTask.activity[GuardedWorkflowExecution]
  }

  private def unguardedActivity(workflowTask: org.silkframework.workspace.ProjectTask[Workflow]) = {
    workflowTask.activity[UnguardedWorkflowExecution]
  }

  private def selectionActivity(workflowTask: org.silkframework.workspace.ProjectTask[Workflow]) = {
    workflowTask.activity[SelectionWorkflowExecution]
  }

  private def guardedControl[T](workflowTask: org.silkframework.workspace.ProjectTask[Workflow],
                                delegate: ActivityControl[T]): QueuedActivityControl[T] = {
    new QueuedActivityControl(
      delegate = delegate,
      task = Some(workflowTask),
      factory = guardedFactory,
      limit = workflowLimit
    )
  }

  private def testUserContext(userUri: String): UserContext = TestWorkflowUserContext(userUri)

  private def startAndAwaitQueuedOrRunning(control: ActivityControl[_], userContext: UserContext): Unit = {
    control.start()(userContext)
    eventually {
      control.status() match {
        case _: Waiting =>
        case _: Status.Running =>
        case other => fail(s"Expected activity to reach queued or running state after start, but got: $other")
      }
    }
  }

  private def startAndAwaitQueuedOrRunning(activity: WorkspaceActivity[_], userContext: UserContext): String = {
    val instanceId = activity.start()(userContext)
    startAndAwaitQueuedOrRunning(activity.instance(instanceId), userContext)
    instanceId
  }

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

private case class ValueProducingActivity(finalValue: String) extends Activity[String] {
  private val startedLatch = new CountDownLatch(1)
  private val releaseLatch = new CountDownLatch(1)

  override def initialValue: Option[String] = Some("delegate-initial")

  override def run(context: ActivityContext[String])(implicit userContext: UserContext): Unit = {
    startedLatch.countDown()
    if(!releaseLatch.await(30, TimeUnit.SECONDS)) {
      throw new AssertionError("ValueProducingActivity was not released within the test timeout.")
    }
    context.value.update(finalValue)
  }

  def awaitStarted(): Unit = {
    if(!startedLatch.await(10, TimeUnit.SECONDS)) {
      throw new AssertionError("ValueProducingActivity did not start in time.")
    }
  }

  def release(): Unit = {
    releaseLatch.countDown()
  }
}

private case class TestFailureException(message: String) extends RuntimeException(message)

private case class FailingActivity(exception: TestFailureException) extends Activity[Unit] {
  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    throw exception
  }
}

private case class InterruptibleActivity() extends Activity[Unit] {
  private val startedLatch = new CountDownLatch(1)
  private val releaseLatch = new CountDownLatch(1)
  val cancelInvocations: AtomicInteger = new AtomicInteger(0)

  override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = {
    startedLatch.countDown()
    releaseLatch.await()
  }

  override def cancelExecution()(implicit userContext: UserContext): Unit = {
    cancelInvocations.incrementAndGet()
    super.cancelExecution()
  }

  def awaitStarted(): Unit = {
    if(!startedLatch.await(10, TimeUnit.SECONDS)) {
      throw new AssertionError("InterruptibleActivity did not start in time.")
    }
  }

  def release(): Unit = {
    releaseLatch.countDown()
  }
}

private trait GuardedWorkflowExecution extends HasValue {
  override type ValueType = Unit
}

private trait UnguardedWorkflowExecution extends HasValue {
  override type ValueType = Unit
}

private trait SelectionWorkflowExecution extends HasValue {
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

@Plugin(
  id = "ExecuteWorkflowForTestWithoutLimit",
  label = "Execute without limit",
  categories = Array("WorkflowExecution")
)
private case class UnguardedWorkflowExecutionFactory() extends TaskActivityFactory[Workflow, UnguardedWorkflowExecution] {
  override def isSingleton: Boolean = false

  override def apply(task: ProjectTask[Workflow]): Activity[Unit] = QueueControlledActivity()
}

@Plugin(
  id = "ExecuteWorkflowWithPayloadForSelectionTest",
  label = "Execute with payload for selection test",
  categories = Array("WorkflowExecution")
)
private case class SelectionWorkflowExecutionFactory() extends TaskActivityFactory[Workflow, SelectionWorkflowExecution] {
  override def isSingleton: Boolean = false

  override def apply(task: ProjectTask[Workflow]): Activity[Unit] = QueueControlledActivity()
}

@Plugin(
  id = "NoLimitSelectionWorkflowExecutionActivityLimit",
  label = "No limit selection workflow execution activity limit"
)
private case class NoLimitSelectionWorkflowExecutionActivityLimit() extends ActivityLimit {
  override def limitFor(task: Option[org.silkframework.workspace.ProjectTask[_ <: org.silkframework.config.TaskSpec]],
                        factory: WorkspaceActivityFactory): Option[Int] = {
    task.collect {
      case workflowTask if workflowTask.data.isInstanceOf[Workflow] && factory.pluginSpec.id.toString == "ExecuteWorkflowWithPayloadForSelectionTest" =>
        None
    }.flatten
  }

  override def limiterKey(projectId: Option[Identifier], taskId: Option[Identifier]): ActivityLimiterKey = {
    ActivityLimiterKey(projectId, taskId, "selection-workflow-execution")
  }
}

@Plugin(
  id = "SelectionWorkflowExecutionActivityLimit",
  label = "Selection workflow execution activity limit"
)
private case class SelectionWorkflowExecutionActivityLimit() extends ActivityLimit {
  override def limitFor(task: Option[org.silkframework.workspace.ProjectTask[_ <: org.silkframework.config.TaskSpec]],
                        factory: WorkspaceActivityFactory): Option[Int] = {
    task.collect {
      case workflowTask if workflowTask.data.isInstanceOf[Workflow] && factory.pluginSpec.id.toString == "ExecuteWorkflowWithPayloadForSelectionTest" =>
        Some(1)
    }.flatten
  }

  override def limiterKey(projectId: Option[Identifier], taskId: Option[Identifier]): ActivityLimiterKey = {
    ActivityLimiterKey(projectId, taskId, "selection-workflow-execution")
  }
}

private class PrioritizationProbeControl extends ActivityControl[Unit] {
  private val statusHolder = new ValueHolder[Status](Some(Status.Idle()))
  private val valueHolder = new ValueHolder[Unit](Some(()))
  private val startedLatch = new CountDownLatch(1)
  private val finishedLatch = new CountDownLatch(1)
  val prioritizedCalls: AtomicInteger = new AtomicInteger(0)

  override def name: String = "Prioritization probe control"

  override def value: Observable[Unit] = valueHolder

  override def status: Observable[Status] = statusHolder

  override def startedBy: UserContext = UserContext.Empty

  override def children(): Seq[ActivityControl[_]] = Seq.empty

  override def start()(implicit user: UserContext): Unit = {
    statusHolder.update(Status.Running("Running", None))
    startedLatch.countDown()
  }

  override def restart()(implicit user: UserContext): Future[Unit] = Future.successful(())

  override def startBlocking()(implicit user: UserContext): Unit = {
    start()
    waitUntilFinished()
  }

  override def startBlockingAndGetValue(initialValue: Option[Unit])(implicit user: UserContext): Unit = ()

  override def startPrioritized()(implicit user: UserContext): Unit = {
    prioritizedCalls.incrementAndGet()
  }

  override def cancel()(implicit user: UserContext): Unit = {
    finish(cancelled = true)
  }

  override def reset()(implicit userContext: UserContext): Unit = ()

  override def underlying: Activity[Unit] = new Activity[Unit] {
    override def run(context: ActivityContext[Unit])(implicit userContext: UserContext): Unit = ()
  }

  override def waitUntilFinished(): Unit = {
    finishedLatch.await(10, TimeUnit.SECONDS)
  }

  def awaitStarted(): Unit = {
    if(!startedLatch.await(10, TimeUnit.SECONDS)) {
      throw new AssertionError("PrioritizationProbeControl did not start in time.")
    }
  }

  def finish(cancelled: Boolean = false): Unit = {
    statusHolder.update(Status.Finished(success = !cancelled, runtime = 0L, cancelled = cancelled))
    finishedLatch.countDown()
  }
}
