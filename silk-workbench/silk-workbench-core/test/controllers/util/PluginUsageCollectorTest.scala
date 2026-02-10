package controllers.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{CustomTask, FixedNumberOfInputs, InputPorts, Port}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.{Aggregation, Comparison}
import org.silkframework.rule.plugins.aggegrator.MinimumAggregator
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.{ComplexMapping, DatasetSelection, LinkSpec, LinkageRule, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.DPair
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowBuilder}

class PluginUsageCollectorTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "PluginUsageCollector"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  it should "collect plugin usages from a dataset task" in {
    val project = retrieveOrCreateProject("datasetProject")
    val resource = project.resources.get("test.csv")
    resource.writeString("id,name\n1,test")

    val datasetId = "csvDataset"
    project.addTask(datasetId, DatasetSpec(CsvDataset(resource)))

    val task = project.anyTask(datasetId)
    val usages = PluginUsageCollector.pluginUsages(task)

    usages.size mustBe 1
    usages.head.pluginId mustBe "csv"
    usages.head.project mustBe Some("datasetProject")
    usages.head.task mustBe Some(datasetId)
  }

  it should "collect plugin usages from a custom task" in {
    val project = retrieveOrCreateProject("customTaskProject")

    val customTaskId = "testCustomTask"
    project.addTask(customTaskId, TestCustomTask())

    val task = project.anyTask(customTaskId)
    val usages = PluginUsageCollector.pluginUsages(task)

    usages.size mustBe 1
    usages.head.pluginId mustBe "testCustomTask"
    usages.head.project mustBe Some("customTaskProject")
    usages.head.task mustBe Some(customTaskId)
  }

  it should "collect plugin usages from a transform task with transformers" in {
    val project = retrieveOrCreateProject("transformProject")
    val resource = project.resources.get("source.csv")
    resource.writeString("id,name\n1,test")

    val datasetId = "sourceDataset"
    project.addTask(datasetId, DatasetSpec(CsvDataset(resource)))

    val transformId = "transformTask"
    val transformer = ConstantTransformer("value")
    val mapping = RootMappingRule(MappingRules(
      propertyRules = Seq(
        ComplexMapping(
          id = "mapping1",
          operator = TransformInput(id = "transform1", transformer = transformer),
          target = Some(MappingTarget("http://example.org/property"))
        )
      )
    ))
    project.addTask(transformId, TransformSpec(DatasetSelection(datasetId), mapping))

    val task = project.anyTask(transformId)
    val usages = PluginUsageCollector.pluginUsages(task)

    usages.size mustBe 1
    usages.head.pluginId mustBe "constant"
    usages.head.project mustBe Some("transformProject")
    usages.head.task mustBe Some(transformId)
  }

  it should "collect plugin usages from a linking task" in {
    val project = retrieveOrCreateProject("linkingProject")
    val resource = project.resources.get("source.csv")
    resource.writeString("id,name\n1,test")

    val datasetId = "linkingDataset"
    project.addTask(datasetId, DatasetSpec(CsvDataset(resource)))

    val linkingId = "linkingTask"
    val selection = DatasetSelection(datasetId)

    // Create a linking rule with a comparison using EqualityMetric
    val comparison = Comparison(
      id = "compare1",
      threshold = 0.0,
      metric = EqualityMetric(),
      inputs = DPair(
        PathInput(path = org.silkframework.entity.paths.UntypedPath("name")),
        PathInput(path = org.silkframework.entity.paths.UntypedPath("name"))
      )
    )
    val aggregation = Aggregation(
      id = "aggregate1",
      aggregator = MinimumAggregator(),
      operators = Seq(comparison)
    )
    val linkageRule = LinkageRule(Some(aggregation))
    project.addTask(linkingId, LinkSpec(selection, selection, linkageRule))

    val task = project.anyTask(linkingId)
    val usages = PluginUsageCollector.pluginUsages(task)

    // Should contain EqualityMetric and MinimumAggregator
    usages.size mustBe 2
    usages.map(_.pluginId).toSet mustBe Set("equality", "min")
  }

  it should "collect plugin usages from a workflow by traversing all referenced tasks" in {
    val project = retrieveOrCreateProject("workflowProject")
    val resource = project.resources.get("workflow.csv")
    resource.writeString("id,name\n1,test")

    // Add a dataset task
    val datasetId = "workflowDataset"
    project.addTask(datasetId, DatasetSpec(CsvDataset(resource)))

    // Add a custom task
    val customTaskId = "workflowCustomTask"
    project.addTask(customTaskId, TestCustomTask())

    // Add a transform task
    val transformId = "workflowTransform"
    val transformer = ConstantTransformer("value")
    val mapping = RootMappingRule(MappingRules(
      propertyRules = Seq(
        ComplexMapping(
          id = "mapping1",
          operator = TransformInput(id = "transform1", transformer = transformer),
          target = Some(MappingTarget("http://example.org/property"))
        )
      )
    ))
    project.addTask(transformId, TransformSpec(DatasetSelection(datasetId), mapping))

    // Create workflow that references all tasks
    val workflowId = "testWorkflow"
    val workflow = WorkflowBuilder.create()
      .dataset(datasetId)
      .operator(transformId)
      .operator(customTaskId)
      .build()
    project.addTask(workflowId, workflow)

    val task = project.anyTask(workflowId)
    val usages = PluginUsageCollector.pluginUsages(task)

    // Should contain: csv (dataset), constant (transformer), testCustomTask (custom task)
    usages.size mustBe 3
    usages.map(_.pluginId).toSet mustBe Set("csv", "constant", "testCustomTask")
  }

  it should "collect nested transformer usages" in {
    val project = retrieveOrCreateProject("nestedTransformProject")
    val resource = project.resources.get("nested.csv")
    resource.writeString("id,name\n1,test")

    val datasetId = "nestedDataset"
    project.addTask(datasetId, DatasetSpec(CsvDataset(resource)))

    val transformId = "nestedTransformTask"
    // Create nested transformers: outer transformer wrapping inner transformer
    val innerTransformer = ConstantTransformer("inner")
    val outerTransformer = ConstantTransformer("outer")
    val mapping = RootMappingRule(MappingRules(
      propertyRules = Seq(
        ComplexMapping(
          id = "mapping1",
          operator = TransformInput(
            id = "outer",
            transformer = outerTransformer,
            inputs = Seq(TransformInput(id = "inner", transformer = innerTransformer)).toIndexedSeq
          ),
          target = Some(MappingTarget("http://example.org/property"))
        )
      )
    ))
    project.addTask(transformId, TransformSpec(DatasetSelection(datasetId), mapping))

    val task = project.anyTask(transformId)
    val usages = PluginUsageCollector.pluginUsages(task)

    // Both transformers should be collected (same plugin type, but 2 usages)
    usages.size mustBe 2
    usages.forall(_.pluginId == "constant") mustBe true
  }

  it should "return deprecation message for deprecated plugins" in {
    val project = retrieveOrCreateProject("deprecatedProject")

    val customTaskId = "deprecatedTask"
    project.addTask(customTaskId, DeprecatedTestTask())

    val task = project.anyTask(customTaskId)
    val usages = PluginUsageCollector.pluginUsages(task)

    usages.size mustBe 1
    usages.head.deprecationMessage mustBe Some("This plugin is deprecated for testing purposes.")
  }
}

@Plugin(
  id = "testCustomTask",
  label = "Test Custom Task",
  description = "A custom task for testing plugin usage collection"
)
case class TestCustomTask() extends CustomTask {
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)
  override def outputPort: Option[Port] = None
}

@Plugin(
  id = "deprecatedTestTask",
  label = "Deprecated Test Task",
  description = "A deprecated custom task for testing",
  deprecation = "This plugin is deprecated for testing purposes."
)
case class DeprecatedTestTask() extends CustomTask {
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)
  override def outputPort: Option[Port] = None
}

