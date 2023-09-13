package controllers.workspaceApi

import akka.actor.ActorSystem
import controllers.util.ReportsApiClient
import helper.IntegrationTestTrait
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.silkframework.config._
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, SimpleExecutionReport}
import org.silkframework.runtime.activity.{ActivityContext, ValueHolder}
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow._
import org.silkframework.workspace.{Project, ProjectConfig, ProjectTask, WorkspaceFactory}
import play.api.routing.Router

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, MINUTES}

class ReportsApiTest extends AnyFlatSpec with IntegrationTestTrait with ReportsApiClient with Matchers with Eventually {

  behavior of "Report API"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  private implicit val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(scaled(Span(30, Seconds)))

  it should "provide updates to the current report" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    PluginRegistry.registerPlugin(classOf[TestCustomTaskExecutor])

    // Create workflow with two custom tasks
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig())

    val taskId1 = "workflowTestTask1"
    val task1 = TestCustomTask()
    project.addTask[TestCustomTask](taskId1, task1)

    val taskId2 = "workflowTestTask2"
    val task2 = TestCustomTask()
    project.addTask[TestCustomTask](taskId2, task2)

    val workflowTask = generateWorkflow(project, taskId1, taskId2)

    // Start workflow
    val activity = workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance]
    activity.start()

    // Wait until first task is being executed
    eventually {
      val taskReports = activity.value().report.taskReports
      taskReports must not be empty
      taskReports.head.report.task.data.asInstanceOf[TestCustomTask].reportHolder must not be null
    }

    // Check the initial execution report
    val report = currentReport(project.config.id, workflowTask.id).asInstanceOf[WorkflowExecutionReport]
    val task1Report = report.taskReports.head.report
    task1Report.entityCount mustBe 0
    task1Report.isDone mustBe false

    // Connect to the websocket for updates
    val queue = currentReportUpdatesWebsocket(project.config.id, workflowTask.id)

    // Checks the next update in the queue
    def checkNextUpdate(testFunc: ReportSummary => Unit): Unit = {
      val newStatus = Await.result(queue.pull(), Duration(1, MINUTES)).get
      newStatus.updates.size mustBe 1
      testFunc(newStatus.updates.head)
    }

    // Increase the entity count and check if a new report is pushed
    for(i <- 1 until 3) {
      task1.updateReport(i)
      checkNextUpdate { report =>
        report.node mustBe taskId1
        report.entityCount mustBe i
      }
    }

    // Finish the first task and check the report
    task1.updateReport(3, isDone = true)
    checkNextUpdate { report =>
      report.node mustBe taskId1
      report.entityCount mustBe 3
      report.isDone mustBe true
    }

    // Wait for the second task to start
    val newStatus2 = Await.result(queue.pull(), Duration(1, MINUTES)).get
    checkNextUpdate { report =>
      report.node mustBe taskId2
      report.entityCount mustBe 0
      report.isDone mustBe false
    }

    // Finish the second task and check the report
    task2.updateReport(1, isDone = true)
    checkNextUpdate { report =>
      report.node mustBe taskId2
      report.entityCount mustBe 1
      report.isDone mustBe true
    }

    // Shutdown
    queue.cancel()
  }

  private def generateWorkflow(project: Project, taskId1: Identifier, taskId2: Identifier): ProjectTask[Workflow] = {
    val datasets = ArrayBuffer[WorkflowDataset]()
    val operators = ArrayBuffer[WorkflowOperator]()

    operators +=
      WorkflowOperator(
        inputs = Seq(),
        task = taskId1,
        outputs = Seq(taskId2),
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = taskId1,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    operators +=
      WorkflowOperator(
        inputs = Seq(taskId1),
        task = taskId2,
        outputs = Seq(),
        errorOutputs = Seq.empty,
        position = (0, 0),
        nodeId = taskId2,
        outputPriority = None,
        configInputs = Seq.empty,
        dependencyInputs = Seq.empty
      )

    val workflow = Workflow(WorkflowOperatorsParameter(operators.toSeq), WorkflowDatasetsParameter(datasets.toSeq))
    project.addTask[Workflow]("workflow", workflow)
  }
}

case class TestCustomTask() extends CustomTask {

  @volatile
  var entityCount: Int = 0

  @volatile
  var finish: Boolean = false

  // Will be set by the executor
  @volatile
  var reportHolder: ValueHolder[ExecutionReport] = null

  def updateReport(entityCount: Int, isDone: Boolean = false): Unit = {
    reportHolder.update(SimpleExecutionReport(PlainTask("dummmy", this), summary = Seq.empty, error = None, warnings = Seq.empty, isDone = isDone, entityCount = entityCount, operation = None))
  }

  override def inputPorts: InputPorts = FlexibleNumberOfInputs()

  override def outputPort: Option[Port] = Some(UnknownSchemaPort)
}

case class TestCustomTaskExecutor() extends LocalExecutor[TestCustomTask] {
  override def execute(task: Task[TestCustomTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    task.data.reportHolder = context.value
    task.data.updateReport(0, isDone = false)
    while(!context.value().isDone) {
      Thread.sleep(200)
    }
    None
  }

}
