package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonToFileOperator}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{InMemoryResourceManager, WritableResource}
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
    val output = runWorkflow(
      JsonToFileOperator(inputPath = "jsonContent"),
      s"""[{"jsonContent": ${Json.stringify(JsString(inputJson))}}]"""
    )
    output.loadAsString() mustBe inputJson
  }

  it should "pack multiple input entities into a single ZIP file when zipOutput is enabled" in {
    val json1 = """{"value":"first"}"""
    val json2 = """{"value":"second"}"""
    val output = runWorkflow(
      JsonToFileOperator(inputPath = "jsonContent", zipOutput = true),
      s"""[{"jsonContent": ${Json.stringify(JsString(json1))}}, {"jsonContent": ${Json.stringify(JsString(json2))}}]""",
      outputName = "output.zip"
    )
    val zip = new ZipInputStream(output.inputStream)
    val entries = try {
      Iterator.continually(zip.getNextEntry)
        .takeWhile(_ != null)
        .map(entry => (entry.getName, scala.io.Source.fromInputStream(zip, "UTF-8").mkString))
        .toSeq
    } finally {
      zip.close()
    }
    entries.size mustBe 2
    entries(0) mustBe (("entry-0.json", json1))
    entries(1) mustBe (("entry-1.json", json2))
  }
}

object LocalJsonToFileWorkflowTest {
  val SourceDatasetId = "sourceDataset"
  val JsonToFileId = "jsonToFile"
  val OutputDatasetId = "outputDataset"
  val WorkflowId = "workflow"

  /** Builds an in-memory workspace with a three-node workflow (source JSON dataset → JsonToFile operator →
   * output dataset), runs it synchronously, and returns the output resource for assertion. */
  def runWorkflow(operator: JsonToFileOperator, sourceContent: String, outputName: String = "output.json")
                 (implicit userContext: UserContext, pluginContext: PluginContext, prefixes: Prefixes): WritableResource = {
    val resources = InMemoryResourceManager()

    val sourceResource = resources.get("source.json")
    sourceResource.writeString(sourceContent)

    val outputResource = resources.get(outputName)

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProject"))))

    project.addTask(SourceDatasetId, DatasetSpec(JsonDataset(sourceResource)))
    project.addTask(JsonToFileId, operator)
    project.addTask(OutputDatasetId, DatasetSpec(JsonDataset(outputResource)))

    val sourceNode = WorkflowDataset(
      inputs = Seq.empty, task = SourceDatasetId, outputs = Seq(JsonToFileId),
      position = (0, 0), nodeId = SourceDatasetId, outputPriority = None,
      configInputs = Seq.empty, dependencyInputs = Seq.empty
    )
    val operatorNode = WorkflowOperator(
      inputs = Seq(Some(SourceDatasetId)), task = JsonToFileId, outputs = Seq(OutputDatasetId),
      errorOutputs = Seq.empty, position = (100, 0), nodeId = JsonToFileId,
      outputPriority = None, configInputs = Seq.empty, dependencyInputs = Seq.empty
    )
    val outputNode = WorkflowDataset(
      inputs = Seq(Some(JsonToFileId)), task = OutputDatasetId, outputs = Seq.empty,
      position = (200, 0), nodeId = OutputDatasetId, outputPriority = None,
      configInputs = Seq.empty, dependencyInputs = Seq.empty
    )

    project.addTask(WorkflowId, Workflow(
      operators = Seq(operatorNode),
      datasets = Seq(sourceNode, outputNode)
    ))

    project.task[Workflow](WorkflowId)
      .activity[LocalWorkflowExecutorGeneratingProvenance]
      .startBlocking()

    outputResource
  }
}
