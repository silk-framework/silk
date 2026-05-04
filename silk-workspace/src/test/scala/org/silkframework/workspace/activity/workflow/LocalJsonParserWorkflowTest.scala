package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonParserTask}
import play.api.libs.json.{JsString, Json}
import LocalJsonParserWorkflowTest._

/**
 * Integration test for the Parse JSON operator in a workflow context.
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
class LocalJsonParserWorkflowTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
  )

  behavior of "Parse JSON operator in a workflow"

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

  it should "write parsed entities to a downstream JSON dataset" in {
    val resources = InMemoryResourceManager()

    // Source: one entity whose "jsonContent" field holds the JSON blob Parse JSON will parse
    val sourceResource = resources.get("source.json")
    sourceResource.writeString(s"""[{"jsonContent": ${Json.stringify(JsString(inputJson))}}]""")

    // Output: empty file to be written by the workflow
    val outputResource = resources.get("output.json")

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProject"))))

    project.addTask(SourceDatasetId, DatasetSpec(JsonDataset(sourceResource)))
    project.addTask(ParseJsonId, JsonParserTask(inputPath = "jsonContent", basePath = "persons"))
    project.addTask(OutputDatasetId, DatasetSpec(JsonDataset(outputResource)))

    val sourceNode = WorkflowDataset(
      inputs = Seq.empty,
      task = SourceDatasetId,
      outputs = Seq(ParseJsonId),
      position = (0, 0),
      nodeId = SourceDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val parseNode = WorkflowOperator(
      inputs = Seq(Some(SourceDatasetId)),
      task = ParseJsonId,
      outputs = Seq(OutputDatasetId),
      errorOutputs = Seq.empty,
      position = (100, 0),
      nodeId = ParseJsonId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )
    val outputNode = WorkflowDataset(
      inputs = Seq(Some(ParseJsonId)),
      task = OutputDatasetId,
      outputs = Seq.empty,
      position = (200, 0),
      nodeId = OutputDatasetId,
      outputPriority = None,
      configInputs = Seq.empty,
      dependencyInputs = Seq.empty
    )

    project.addTask(WorkflowId, Workflow(
      operators = Seq(parseNode),
      datasets = Seq(sourceNode, outputNode)
    ))

    val workflowTask = project.task[Workflow](WorkflowId)
    workflowTask.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    outputResource.size.getOrElse(0L) should be > 0L

    val names = (Json.parse(outputResource.loadAsString()) \\ "name").flatMap(_.as[Seq[String]])
    names should contain allOf ("John", "Max")
  }
}

object LocalJsonParserWorkflowTest {
  val SourceDatasetId = "sourceDataset"
  val ParseJsonId = "parseJson"
  val OutputDatasetId = "outputDataset"
  val WorkflowId = "workflow"
}
