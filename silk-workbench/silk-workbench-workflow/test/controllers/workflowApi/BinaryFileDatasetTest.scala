package controllers.workflowApi

import akka.stream.scaladsl.{FileIO, Source}
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.operations.{AddProjectFilesOperator, GetProjectFilesOperator, OverwriteStrategyEnum}
import org.silkframework.plugins.dataset.BinaryFileDataset
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowBuilder}
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

import java.nio.file.Files

class BinaryFileDatasetTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait with IntegrationTestTrait with ApiClient {

  behavior of "BinaryFileDataset"

  implicit val pluginContext: PluginContext = TestPluginContext()

  it should "support reading file entities" in {
    val project = retrieveOrCreateProject("BinaryFileDatasetTest1")

    // Create input dataset
    val inputDatasetId = "inputDataset"
    val inputDatasetData = "Test data"
    val inputFile = project.resources.get("test.csv")
    inputFile.writeString(inputDatasetData)
    project.addTask(inputDatasetId, DatasetSpec(BinaryFileDataset(inputFile)))

    // Add to project operator
    val addTaskId = "addTask"
    val newFileName = "writtenFile"
    project.addTask(addTaskId, AddProjectFilesOperator(fileName = newFileName, overwriteStrategy = OverwriteStrategyEnum.overwrite))

    // Build and execute workflow
    val workflow = WorkflowBuilder.create().dataset(inputDatasetId).operator(addTaskId).replaceableInputs(Seq(inputDatasetId)).build()
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check the result of the workflow execution
    project.resources.get(newFileName).loadAsString() shouldBe inputDatasetData

    // Execute a variable workflow request with different data
    val payloadContent = "Payload content"
    val payloadFile = Files.createTempFile("payload", "txt")
    Files.writeString(payloadFile, payloadContent)
    val path = controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultPost(project.id, workflowTask.id).url
    val request = client.url(s"$baseUrl$path")
    checkResponse(request.post(Source(
      FilePart("payload", "payload.txt", Option(BinaryFileDataset.mimeType), FileIO.fromPath(payloadFile)) :: Nil
    )))
    Files.delete(payloadFile)

    // Check the result of the variable workflow request
    project.resources.get(newFileName).loadAsString() shouldBe payloadContent
  }

  it should "support writing file entities" in {
    val project = retrieveOrCreateProject("BinaryFileDatasetTest2")

    // Get project operator
    val getFileTaskId = "getFile"
    val fileName = "file"
    val inputFile = project.resources.get(fileName)
    val content = "Test data"
    inputFile.writeString(content)
    project.addTask(getFileTaskId, GetProjectFilesOperator(fileName = fileName))

    // Create output dataset
    val outputDatasetId = "outputDataset"
    val outputFile = project.resources.get("test.csv")
    outputFile.writeString("initial content")
    project.addTask(outputDatasetId, DatasetSpec(BinaryFileDataset(outputFile)))

    // Build and execute workflow
    val workflow = WorkflowBuilder.create().operator(getFileTaskId).dataset(outputDatasetId).replaceableOutputs(Seq(outputDatasetId)).build()
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check result
    outputFile.loadAsString() shouldBe content

    // Execute a variable workflow request with different data
    val newContent = "New content"
    inputFile.writeString(newContent)
    val path = controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultGet(project.id, workflowTask.id).url
    val request = client.url(s"$baseUrl$path").addHttpHeaders(ACCEPT -> BinaryFileDataset.mimeType)
    val response = checkResponse(request.get())
    response.body shouldBe newContent
  }

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowApi.Routes])

  override def workspaceProviderId: String = "inMemory"
}
