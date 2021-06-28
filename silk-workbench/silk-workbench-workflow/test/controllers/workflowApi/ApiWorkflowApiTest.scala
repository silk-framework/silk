package controllers.workflowApi

import controllers.workflowApi.workflow.{WorkflowNodePortConfig, WorkflowNodesPortConfig}
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.CustomTask
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.Uri
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.routing.Router
import org.silkframework.runtime.plugin._

class ApiWorkflowApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with MustMatchers {
  behavior of "Workflow API"

  override def projectPathInClasspath: String = "2dc191ef-d583-4eb8-a8ed-f2a3fb94bd8f_WorkflowAPItestproject.zip"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[workflowApi.Routes])

  override def workspaceProviderId: String = "inMemory"

  private val workflowId = "f506c6d7-1c83-424e-8033-8d5137b704b9_Workflow"
  private val customTaskWithoutSchemaFromInitialProject = "23586f0a-037d-4acd-91ad-669afe05a074_JSONparser"
  private val customTaskWithSchemaFromInitialProject = "a5b07467-ace6-4af3-a09c-de4e61f12e30_CopyofJSONparser"

  it should "return a correct workflow nodes port config" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    project.addTask("noSchema", TestCustomTask(None))
    project.addTask("onePort", TestCustomTask(Some(1)))
    project.addTask("fourPort", TestCustomTask(Some(4)))
    val responseJson = checkResponse(createRequest(controllers.workflowApi.routes.ApiWorkflowApi.workflowNodesConfig(projectId, workflowId)).get()).json
    val portConfig = JsonHelpers.fromJsonValidated[WorkflowNodesPortConfig](responseJson)
    val noSchemaConfig = Some(WorkflowNodePortConfig(1, None))
    portConfig.byTaskId.get(customTaskWithoutSchemaFromInitialProject) mustBe noSchemaConfig
    portConfig.byTaskId.get(customTaskWithSchemaFromInitialProject) mustBe Some(WorkflowNodePortConfig(1, Some(1)))
    portConfig.byTaskId.get("noSchema") mustBe noSchemaConfig
    portConfig.byTaskId.get("onePort") mustBe Some(WorkflowNodePortConfig(1, Some(1)))
    portConfig.byTaskId.get("fourPort") mustBe Some(WorkflowNodePortConfig(4, Some(4)))
  }
}

case class TestCustomTask(nrPorts: IntOptionParameter) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = nrPorts map { nr =>
    for(_ <- 1 to nr) yield {
      EntitySchema(Uri("uri"), typedPaths = IndexedSeq.empty)
    }
  }

  override def outputSchemaOpt: Option[EntitySchema] = None
}