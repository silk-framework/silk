package controllers.workflowApi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.operations.{AddProjectFilesOperator, GetProjectFilesOperator}
import org.silkframework.plugins.dataset.BinaryFileDataset
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowBuilder}

class BinaryFileDatasetTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "BinaryFileDataset"

  implicit val pluginContext: PluginContext = TestPluginContext()

  it should "support reading file entities" in {
    val project = retrieveOrCreateProject("BinaryFileDatasetTest1")

    // Create input dataset
    val inputDatasetId = "inputDataset"
    val content = "Test data"
    val inputFile = project.resources.get("test.csv")
    inputFile.writeString(content)
    project.addTask(inputDatasetId, DatasetSpec(BinaryFileDataset(inputFile)))

    // Add to project operator
    val addTaskId = "addTask"
    val newFileName = "writtenFile"
    project.addTask(addTaskId, AddProjectFilesOperator(fileName = newFileName))

    // Build and execute workflow
    val workflow = WorkflowBuilder.inputOutput(inputDatasetId, addTaskId)
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check result
    project.resources.get(newFileName).loadAsString() shouldBe content
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
    val workflow = WorkflowBuilder.inputOutput(getFileTaskId, outputDatasetId)
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check result
    outputFile.loadAsString() shouldBe content
  }

  override def workspaceProviderId: String = "inMemory"
}
