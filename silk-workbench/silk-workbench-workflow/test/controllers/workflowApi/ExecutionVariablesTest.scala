package controllers.workflowApi

import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.plugin._
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.routing.Router

import scala.concurrent.Future

/**
  * Tests that workflow execution variables can be passed via the API and are available during workflow execution.
  */
class ExecutionVariablesTest extends AnyFlatSpec with BeforeAndAfterAll
    with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with ConfigTestTrait with Matchers {

  behavior of "Workflow execution variables"

  override def projectPathInClasspath: String = "diProjects/workflow-execution-integration-test-project.zip"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowExecution.Routes])

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some("jinja")
  )

  private val csvDatasetId = "csvInput"
  private val templateOpId = "templateOp"
  private val outputDatasetId = "outputDataset"
  private val templateWorkflowId = "templateWorkflow"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)

    // Set up a TemplateOperator workflow: CSV → TemplateOperator → InternalDataset
    project.resources.get("input.csv").writeString("value\nhello\n")
    createCsvFileDataset(projectId, csvDatasetId, "input.csv")
    val templateOpParams = ParameterValues(Map(
      "template" -> ParameterStringValue("Result: {{value}}"),
      "language" -> ParameterStringValue("jinja"),
      "outputAttribute" -> ParameterStringValue("output")
    ))
    project.addTask[CustomTask](templateOpId, PluginRegistry.create[CustomTask]("Template", templateOpParams))
    project.addTask[GenericDatasetSpec](outputDatasetId, DatasetSpec(InternalDataset()))
    project.addTask(templateWorkflowId, WorkflowBuilder.transform(csvDatasetId, templateOpId, outputDatasetId))
  }

  it should "execute a workflow with a TemplateOperator while passing execution variables" in {
    checkResponse(executeWorkflowWithVars(templateWorkflowId, Map("testVar" -> "expectedValue123")))

    // Read the output from the InternalDataset to verify the TemplateOperator evaluated correctly
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    val outputEntities = project.task[GenericDatasetSpec](outputDatasetId).data.source.retrieve(
      org.silkframework.entity.EntitySchema.empty
    ).entities.toSeq
    outputEntities must not be empty
    outputEntities.head.values.flatten must contain("Result: hello")
  }

  /** Executes a workflow via the PUT endpoint, optionally passing execution variables. */
  private def executeWorkflowWithVars(workflowId: String, workflowVars: Map[String, String] = Map.empty): Future[WSResponse] = {
    val url = controllers.workflow.routes.WorkflowApi.executeWorkflow(projectId, workflowId).url
    val request = client.url(s"$baseUrl$url")
    if (workflowVars.nonEmpty) {
      request
        .addHttpHeaders("content-type" -> "application/json")
        .put(Json.obj("workflowVariables" -> workflowVars))
    } else {
      request.put("")
    }
  }
}

