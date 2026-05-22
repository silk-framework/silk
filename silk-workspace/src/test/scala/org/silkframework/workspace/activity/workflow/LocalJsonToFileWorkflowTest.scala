package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonToFileOperator}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}
import play.api.libs.json.{JsString, Json}
import LocalJsonToFileWorkflowTest._

import java.util.zip.ZipInputStream

/**
 * Integration test for the JSON to File operator in a workflow context.
 *
 * When running from IntelliJ, add the following to the run configuration VM options:
 *   --add-opens=java.base/java.nio=ALL-UNNAMED
 *
 * lmdbjava (the Java binding for LMDB, used by the persistent caching layer initialised
 * as part of the workspace infrastructure) accesses java.nio.Buffer.address via reflection
 * to obtain raw memory addresses for direct buffers. Since Java 9 the module system blocks
 * this by default, causing an InaccessibleObjectException before the test assertions run.
 * The flag opens the java.nio package to unnamed modules (classpath code), restoring the
 * access lmdbjava requires. sbt picks this up automatically from build.sbt; IntelliJ does not.
 */
class LocalJsonToFileWorkflowTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
  )

  behavior of "JSON to File operator in a workflow"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val pluginContext: PluginContext = PluginContext.empty
  implicit val prefixes: Prefixes = Prefixes.empty

  private val inputJson =
    """{
      |  "persons": [
      |    { "name": "John" },
      |    { "name": "Max" }
      |  ]
      |}""".stripMargin

  it should "write the JSON string on an input entity to a file in a downstream dataset" in {
    val resources = InMemoryResourceManager()

    // Source: one entity whose "jsonContent" field holds the JSON blob
    val sourceResource = resources.get("source.json")
    sourceResource.writeString(s"""[{"jsonContent": ${Json.stringify(JsString(inputJson))}}]""")

    // Output: empty file the downstream dataset will receive from JSON to File
    val outputResource = resources.get("output.json")

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProject"))))

    project.addTask(SourceDatasetId, DatasetSpec(JsonDataset(sourceResource)))
    project.addTask(JsonToFileId, JsonToFileOperator(inputPath = "jsonContent"))
    project.addTask(OutputDatasetId, DatasetSpec(JsonDataset(outputResource)))

    val sourceNode = WorkflowDataset(
      inputs = Seq.empty,
      task = SourceDatasetId,
      outputs = Seq(JsonToFileId),
      position = (0, 0),
      nodeId = SourceDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val operatorNode = WorkflowOperator(
      inputs = Seq(Some(SourceDatasetId)),
      task = JsonToFileId,
      outputs = Seq(OutputDatasetId),
      errorOutputs = Seq.empty,
      position = (100, 0),
      nodeId = JsonToFileId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val outputNode = WorkflowDataset(
      inputs = Seq(Some(JsonToFileId)),
      task = OutputDatasetId,
      outputs = Seq.empty,
      position = (200, 0),
      nodeId = OutputDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )

    project.addTask(WorkflowId, Workflow(
      operators = Seq(operatorNode),
      datasets = Seq(sourceNode, outputNode)
    ))

    val workflowTask = project.task[Workflow](WorkflowId)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    outputResource.loadAsString() shouldBe inputJson
  }

  it should "pack multiple input entities into a single ZIP file when zipOutput is enabled" in {
    val resources = InMemoryResourceManager()

    val json1 = """{"value":"first"}"""
    val json2 = """{"value":"second"}"""

    val sourceResource = resources.get("source.json")
    sourceResource.writeString(
      s"""[{"jsonContent": ${Json.stringify(JsString(json1))}}, {"jsonContent": ${Json.stringify(JsString(json2))}}]"""
    )

    val outputResource = resources.get("output.zip")

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProjectZip"))))

    project.addTask(SourceDatasetId, DatasetSpec(JsonDataset(sourceResource)))
    project.addTask(JsonToFileId, JsonToFileOperator(inputPath = "jsonContent", zipOutput = true))
    project.addTask(OutputDatasetId, DatasetSpec(JsonDataset(outputResource)))

    val sourceNode = WorkflowDataset(
      inputs = Seq.empty,
      task = SourceDatasetId,
      outputs = Seq(JsonToFileId),
      position = (0, 0),
      nodeId = SourceDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val operatorNode = WorkflowOperator(
      inputs = Seq(Some(SourceDatasetId)),
      task = JsonToFileId,
      outputs = Seq(OutputDatasetId),
      errorOutputs = Seq.empty,
      position = (100, 0),
      nodeId = JsonToFileId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val outputNode = WorkflowDataset(
      inputs = Seq(Some(JsonToFileId)),
      task = OutputDatasetId,
      outputs = Seq.empty,
      position = (200, 0),
      nodeId = OutputDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )

    project.addTask(WorkflowId, Workflow(
      operators = Seq(operatorNode),
      datasets = Seq(sourceNode, outputNode)
    ))

    val workflowTask = project.task[Workflow](WorkflowId)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    val zip = new ZipInputStream(outputResource.inputStream)
    val entries = try {
      Iterator.continually(zip.getNextEntry)
        .takeWhile(_ != null)
        .map { entry =>
          val content = scala.io.Source.fromInputStream(zip, "UTF-8").mkString
          (entry.getName, content)
        }.toSeq
    } finally {
      zip.close()
    }

    entries.size shouldBe 2
    entries(0) shouldBe (("entry-0.json", json1))
    entries(1) shouldBe (("entry-1.json", json2))
  }
}

object LocalJsonToFileWorkflowTest {
  val SourceDatasetId = "sourceDataset"
  val JsonToFileId = "jsonToFile"
  val OutputDatasetId = "outputDataset"
  val WorkflowId = "workflow"
}
