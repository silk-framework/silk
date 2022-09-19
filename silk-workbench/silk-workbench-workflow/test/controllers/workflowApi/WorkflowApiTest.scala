package controllers.workflowApi

import controllers.workflowApi.workflow.{WorkflowNodePortConfig, WorkflowNodesPortConfig}
import helper.IntegrationTestTrait
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{CustomTask, Prefixes, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionType, Executor, ExecutorOutput}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin._
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.{ConfigTestTrait, Uri}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowOperator, WorkflowOperatorsParameter}
import play.api.routing.Router

class WorkflowApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with ConfigTestTrait with IntegrationTestTrait with MustMatchers {
  behavior of "Workflow API"

  override def projectPathInClasspath: String = "2dc191ef-d583-4eb8-a8ed-f2a3fb94bd8f_WorkflowAPItestproject.zip"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowApi.Routes])

  override def workspaceProviderId: String = "inMemory"

  private val workflowId = "f506c6d7-1c83-424e-8033-8d5137b704b9_Workflow"
  private val customTaskWithoutSchemaFromInitialProject = "23586f0a-037d-4acd-91ad-669afe05a074_JSONparser"
  private val customTaskWithSchemaFromInitialProject = "a5b07467-ace6-4af3-a09c-de4e61f12e30_CopyofJSONparser"

  it should "return a correct workflow nodes port config" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    project.addTask("noSchema", TestCustomTask(None))
    project.addTask("onePort", TestCustomTask(Some(1)))
    project.addTask("fourPort", TestCustomTask(Some(4)))
    val responseJson = checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowNodesConfig(projectId, workflowId)).get()).json
    val portConfig = JsonHelpers.fromJsonValidated[WorkflowNodesPortConfig](responseJson)
    val noSchemaConfig = Some(WorkflowNodePortConfig(1, None))
    portConfig.byTaskId.get(customTaskWithoutSchemaFromInitialProject) mustBe noSchemaConfig
    portConfig.byTaskId.get(customTaskWithSchemaFromInitialProject) mustBe Some(WorkflowNodePortConfig(1, Some(1)))
    portConfig.byTaskId.get("noSchema") mustBe noSchemaConfig
    portConfig.byTaskId.get("onePort") mustBe Some(WorkflowNodePortConfig(1, Some(1)))
    portConfig.byTaskId.get("fourPort") mustBe Some(WorkflowNodePortConfig(4, Some(4)))
  }

  it should "return a 503 when too many concurrent variable workflows are started" in {
    PluginRegistry.registerPlugin(classOf[BlockingTask])
    PluginRegistry.registerPlugin(classOf[BlockingTaskExecutor])
    val blockingTaskId = "blockingTask"
    project.addTask(blockingTaskId, BlockingTask())
    val workflowId = "concurrentWorkflow"
    project.addTask(workflowId, Workflow(
      operators = WorkflowOperatorsParameter(Seq(WorkflowOperator(
        inputs = Seq.empty,
        task = blockingTaskId,
        outputs = Seq.empty,
        errorOutputs = Seq(),
        position = (100, 100),
        blockingTaskId,
        None,
        Seq.empty
      )))
    ))
    def executeWorkflowAsync(expectedResponseCode: Int): Unit = {
      val url = controllers.workflowApi.routes.WorkflowApi.executeVariableWorkflowAsync(projectId, workflowId).url
      val response = client.url(s"$baseUrl$url")
        .withHttpHeaders(CONTENT_TYPE -> "text/csv")
        .post("")
      checkResponseExactStatusCode(response, expectedResponseCode)
    }
    for(_ <- 1 to WorkspaceActivity.maxConcurrentExecutionsPerActivity()) {
      executeWorkflowAsync(CREATED)
    }
    executeWorkflowAsync(SERVICE_UNAVAILABLE)
    BlockingTask.block = false
    eventually {
      BlockingTask.finishCounter must not be 0
    }
    executeWorkflowAsync(CREATED)
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    WorkspaceActivity.MAX_CONCURRENT_EXECUTIONS_CONFIG_KEY -> Some("2")
  )
}

case class TestCustomTask(nrPorts: IntOptionParameter) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = nrPorts map { nr =>
    for(_ <- 1 to nr) yield {
      EntitySchema(Uri("uri"), typedPaths = IndexedSeq.empty)
    }
  }

  override def outputSchemaOpt: Option[EntitySchema] = None
}

object BlockingTask {
  @volatile
  var block = true
  @volatile
  var finishCounter = 0
}

/** Task that blocks until externally released. */
case class BlockingTask() extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}

case class BlockingTaskExecutor() extends Executor[BlockingTask, ExecutionType] {
  override def execute(task: Task[BlockingTask],
                       inputs: Seq[ExecutionType#DataType],
                       output: ExecutorOutput,
                       execution: ExecutionType,
                       context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[ExecutionType#DataType] = {
    while(BlockingTask.block) {
      Thread.sleep(1)
    }
    BlockingTask.finishCounter += 1
    None
  }
}