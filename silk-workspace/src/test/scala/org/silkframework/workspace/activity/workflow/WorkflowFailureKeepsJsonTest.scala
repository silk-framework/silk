package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.json.{JsonDataset, JsonParserTask}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}
import play.api.libs.json.{JsString, Json}

/**
 * When a workflow run fails before the JSON sink has replaced the output file, the unforced clear at the start of the
 * run must not have removed the previous content of that file.
 * By default, the workflow executor clears the output datasets of the workflow at the start of a run, without forcing
 * the clear.
 */
class WorkflowFailureKeepsJsonTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
  )

  behavior of "A workflow that fails before it writes its JSON output"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val pluginContext: PluginContext = PluginContext.empty
  implicit val prefixes: Prefixes = Prefixes.empty

  private val sourceDatasetId = "sourceDataset"
  private val parseJsonId = "parseJson"
  private val outputDatasetId = "outputDataset"
  private val workflowId = "workflow"

  private val previousOutput = """[{"previous": "output"}]"""

  it should "keep the previous content of the output file" in {
    val resources = InMemoryResourceManager()

    // Source: one entity whose "jsonContent" field holds the JSON blob that Parse JSON would parse
    val sourceResource = resources.get("workflowFailureSource.json")
    sourceResource.writeString(s"""[{"jsonContent": ${Json.stringify(JsString("""{"persons": [{"name": "John"}]}"""))}}]""")

    val outputResource = resources.get("workflowFailureOutput.json")
    outputResource.writeString(previousOutput)

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProject"))))

    project.addTask(sourceDatasetId, DatasetSpec(JsonDataset(sourceResource)))
    // Parse JSON cannot feed a dataset directly: it fails when it is executed, before the output dataset is written
    project.addTask(parseJsonId, JsonParserTask(inputPath = "jsonContent", basePath = "persons"))
    project.addTask(outputDatasetId, DatasetSpec(JsonDataset(outputResource)))
    project.addTask[Workflow](workflowId, WorkflowBuilder.transform(sourceDatasetId, parseJsonId, outputDatasetId))

    intercept[WorkflowExecutionException] {
      project.task[Workflow](workflowId).activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()
    }

    outputResource.loadAsString() shouldBe previousOutput
  }
}
