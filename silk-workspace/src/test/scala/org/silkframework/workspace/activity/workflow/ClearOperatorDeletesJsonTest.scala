package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.operations.ClearDatasetOperator
import org.silkframework.plugins.dataset.json.JsonDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}

/**
 * The Clear dataset operator removes the output file of a JSON dataset through a forced clear.
 * The unforced clear at the start of a workflow run leaves the file in place, so the forced clear of the operator is
 * what removes it.
 */
class ClearOperatorDeletesJsonTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
  )

  behavior of "The Clear dataset operator in a workflow"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val pluginContext: PluginContext = PluginContext.empty
  implicit val prefixes: Prefixes = Prefixes.empty

  private val clearOperatorId = "clearOperator"
  private val outputDatasetId = "outputDataset"
  private val workflowId = "workflow"

  it should "remove the output file of a JSON dataset" in {
    val resources = InMemoryResourceManager()

    val outputResource = resources.get("clearOperatorOutput.json")
    outputResource.writeString("""[{"previous": "output"}]""")

    val workspace = new Workspace(
      provider = new InMemoryWorkspaceProvider(),
      repository = ConstantResourceRepository(resources)
    )
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("testProject"))))

    project.addTask(clearOperatorId, ClearDatasetOperator())
    project.addTask(outputDatasetId, DatasetSpec(JsonDataset(outputResource)))
    project.addTask[Workflow](workflowId, WorkflowBuilder.create().operator(clearOperatorId).dataset(outputDatasetId).build())

    outputResource.exists shouldBe true

    project.task[Workflow](workflowId).activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    outputResource.exists shouldBe false
  }
}
