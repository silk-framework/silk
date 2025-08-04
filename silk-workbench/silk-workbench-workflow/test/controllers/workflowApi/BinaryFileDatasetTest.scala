package controllers.workflowApi

import akka.stream.scaladsl.{FileIO, Source}
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{CustomTask, FixedNumberOfInputs, FixedSchemaPort, InputPorts, Port, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.operations.{AddProjectFilesOperator, GetProjectFilesOperator, OverwriteStrategyEnum}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.plugins.dataset.BinaryFileDataset
import org.silkframework.runtime.activity.{ActivityContext, TestUserContextTrait}
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry, TestPluginContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.resource.zip.ZipOutputStreamResource
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowBuilder}
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.collection.mutable
import scala.util.Using

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

    // Conversion operator
    val conversionTaskId = "conversionTask"
    project.addTask(conversionTaskId, ConversionTask())

    // Create output dataset
    val outputDatasetId = "outputDataset"
    val outputFile = project.resources.get("test.csv")
    outputFile.writeString("initial content")
    project.addTask(outputDatasetId, DatasetSpec(BinaryFileDataset(outputFile)))

    // Build and execute workflow
    val workflow = WorkflowBuilder.create().operator(getFileTaskId).operator(conversionTaskId).dataset(outputDatasetId).replaceableOutputs(Seq(outputDatasetId)).build()
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

  it should "support reading multiple file entities into a zip" in {
    val project = retrieveOrCreateProject("BinaryFileDatasetTest3")

    // Create input dataset
    val inputDatasetId = "inputDataset"
    val inputFile = project.resources.get("test.zip")
    val entryNames = createZip(inputFile, numFiles = 3)
    project.addTask(inputDatasetId, DatasetSpec(BinaryFileDataset(inputFile)))

    // Conversion operator
    val conversionTaskId = "conversionTask"
    project.addTask(conversionTaskId, ConversionTask())

    // Add to project operator
    val addTaskId = "addTask"
    val newFileName = "writtenFile.txt"
    project.addTask(addTaskId, AddProjectFilesOperator(fileName = newFileName, overwriteStrategy = OverwriteStrategyEnum.overwrite))

    // Build and execute workflow
    val workflow = WorkflowBuilder.create().dataset(inputDatasetId).operator(conversionTaskId).operator(addTaskId).replaceableInputs(Seq(inputDatasetId)).build()
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check result
    for((entryName, index) <- entryNames.zipWithIndex) {
      val fileName = "writtenFile" + (if (index > 0) s"-$index" else "") + ".txt"
      project.resources.get(fileName).loadAsString() shouldBe entryName
    }
  }

  it should "support writing multiple file entities into a zip" in {
    val project = retrieveOrCreateProject("BinaryFileDatasetTest4")

    // Get files operator
    val getFileTaskId = "getFile"
    val filePrefix = "file"
    val fileNames = for(i <- 0 until 3) yield s"$filePrefix$i"
    for(fileName <- fileNames) {
      val inputFile = project.resources.get(fileName)
      inputFile.writeString(fileName)
    }
    project.addTask(getFileTaskId, GetProjectFilesOperator(filesRegex = s"$filePrefix.+"))

    // Create output dataset
    val outputDatasetId = "outputDataset"
    val outputFile = project.resources.get("test.zip")
    outputFile.writeString("initial content")
    project.addTask(outputDatasetId, DatasetSpec(BinaryFileDataset(outputFile)))

    // Build and execute workflow
    val workflow = WorkflowBuilder.create().operator(getFileTaskId).dataset(outputDatasetId).replaceableOutputs(Seq(outputDatasetId)).build()
    val workflowTask = project.addTask[Workflow]("workflow", workflow)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Check result
    outputFile.read { inputStream =>
      Using.resource(new ZipInputStream(inputStream)) { zipStream =>
        val foundEntries = Iterator.continually(zipStream.getNextEntry).takeWhile(_ != null).map { zipEntry =>
          // The zipped file should contain the file name
          scala.io.Source.fromInputStream(zipStream).mkString shouldBe zipEntry.getName
          // Collect all file names
          zipEntry.getName
        }.toSeq
        foundEntries should contain theSameElementsAs fileNames
      }
    }
  }

  private def createZip(resource: WritableResource, numFiles: Int): Seq[String] = {
    resource.write() { outputStream =>
      Using.resource(new ZipOutputStream(outputStream)) { zipOutput =>
        for(i <- 0 until numFiles) yield {
          val entryName = s"text$i"
          val entryResource = ZipOutputStreamResource(entryName, entryName, zipOutput)
          entryResource.writeString(entryName)
          entryName
        }
      }
    }
  }

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowApi.Routes])

  override def workspaceProviderId: String = "inMemory"

  override def beforeAll(): Unit = {
    PluginRegistry.registerPlugin(classOf[ConversionTask])
    PluginRegistry.registerPlugin(classOf[ConversionTaskExecutor])
    super.beforeAll()
  }
}

/**
 * A custom task that converts file entities to a generic entity table and back.
 * This is used to test the conversion of file entities in workflows.
 */
case class ConversionTask() extends CustomTask {
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq(FixedSchemaPort(FileEntitySchema.schema)))
  override def outputPort: Option[Port] = Some(FixedSchemaPort(FileEntitySchema.schema))
}

case class ConversionTaskExecutor() extends LocalExecutor[ConversionTask] {

  override def execute(task: Task[ConversionTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    // We convert the file entities to a generic entity table, which is then converted back to file entities.
    val e = inputs.head.entities.toSeq
    val genericEntities = GenericEntityTable(e, FileEntitySchema.schema, task)
    val fileEntities = FileEntitySchema.unapply(genericEntities).getOrElse(throw new IllegalArgumentException("Expected FileEntitySchema input"))
    Some(fileEntities)
  }
}

