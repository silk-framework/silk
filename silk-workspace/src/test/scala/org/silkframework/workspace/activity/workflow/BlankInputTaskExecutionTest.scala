package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.MetaData
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule._
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.activity.linking.{ExecuteLinking, LinkingPathsCache}
import org.silkframework.workspace.activity.transform.TransformPathsCache
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.workspace.activity.workflow.WorkflowTest.{dataset, operator}
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, Project, ProjectConfig, Workspace}

/**
  * Tests transform and linking tasks with blank (unconfigured) inputs:
  * they execute inside a workflow via connections and fail with a clear error standalone.
  */
class BlankInputTaskExecutionTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "Transform and linking tasks with blank inputs"

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.reportManager.plugin" -> Some("inMemoryExecutionReportManager")
  )

  implicit val userContext: UserContext = UserContext.Empty
  implicit val pluginContext: PluginContext = PluginContext.empty

  private val sourceDatasetId = "sourceDataset"
  private val outputDatasetId = "outputDataset"

  private def createProject(): Project = {
    val resources = InMemoryResourceManager()
    val sourceResource = resources.get("source.csv")
    sourceResource.writeString("name\nJohn\nMax")
    val workspace = new Workspace(new InMemoryWorkspaceProvider(), ConstantResourceRepository(resources))
    val project = workspace.createProject(ProjectConfig(metaData = MetaData(Some("blankInputProject"))))
    project.addTask[GenericDatasetSpec](sourceDatasetId, DatasetSpec(CsvDataset(sourceResource)))
    project.addTask[GenericDatasetSpec](outputDatasetId, DatasetSpec(CsvDataset(resources.get("output.csv"))))
    project
  }

  private def blankInputTransformSpec: TransformSpec = {
    TransformSpec(
      mappingRule = RootMappingRule(
        rules = MappingRules(
          DirectMapping(id = "name", sourcePath = UntypedPath("name"), mappingTarget = MappingTarget("name", ValueType.STRING))
        )
      )
    )
  }

  private def blankInputLinkSpec: LinkSpec = {
    LinkSpec(
      rule = LinkageRule(
        operator = Some(
          Comparison(
            id = "compareNames",
            metric = EqualityMetric(),
            inputs = PathInput("sourceName", UntypedPath("name")) :: PathInput("targetName", UntypedPath("name")) :: Nil
          )
        ),
        linkType = "urn:test:matches"
      )
    )
  }

  it should "execute a transform task with a blank input inside a workflow" in {
    val project = createProject()
    project.addTask[TransformSpec]("transform", blankInputTransformSpec)
    project.addTask[Workflow]("workflow", Workflow(
      operators = Seq(operator(task = "transform", inputs = Seq(sourceDatasetId), outputs = Seq(outputDatasetId), nodeId = "transform")),
      datasets = Seq(
        dataset(sourceDatasetId, sourceDatasetId, outputs = Seq("transform")),
        dataset(outputDatasetId, outputDatasetId, inputs = Seq("transform"))
      )
    ))

    project.task[Workflow]("workflow").activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    project.resources.get("output.csv").loadLines() shouldBe Seq("name", "John", "Max")
  }

  it should "execute a linking task with blank inputs inside a workflow" in {
    val project = createProject()
    project.addTask[LinkSpec]("linking", blankInputLinkSpec)
    project.addTask[Workflow]("workflow", Workflow(
      operators = Seq(operator(task = "linking", inputs = Seq("sourceNode", "targetNode"), outputs = Seq(outputDatasetId), nodeId = "linking")),
      datasets = Seq(
        dataset(sourceDatasetId, "sourceNode", outputs = Seq("linking")),
        dataset(sourceDatasetId, "targetNode", outputs = Seq("linking")),
        dataset(outputDatasetId, outputDatasetId, inputs = Seq("linking"))
      )
    ))

    project.task[Workflow]("workflow").activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()

    // Every entity links to itself, so the written links must not be empty
    project.resources.get("output.csv").loadLines() should not be empty
  }

  it should "fail with a clear error when a transform task with a blank input is executed standalone" in {
    val project = createProject()
    project.addTask[TransformSpec]("transform", blankInputTransformSpec)
    val error = intercept[ValidationException] {
      project.task[TransformSpec]("transform").activity[ExecuteTransform].startBlocking()
    }
    error.getMessage should include ("No input source has been configured")
  }

  it should "fail with a clear error when a linking task with blank inputs is executed standalone" in {
    val project = createProject()
    project.addTask[LinkSpec]("linking", blankInputLinkSpec)
    val error = intercept[ValidationException] {
      project.task[LinkSpec]("linking").activity[ExecuteLinking].startBlocking()
    }
    error.getMessage should include ("No source input has been configured")
  }

  it should "load the paths caches of blank input tasks without errors" in {
    val project = createProject()
    project.addTask[TransformSpec]("transform", blankInputTransformSpec)
    project.addTask[LinkSpec]("linking", blankInputLinkSpec)

    // The caches auto-start on addTask, so wait for that run instead of racing it with a second start
    val transformCache = project.task[TransformSpec]("transform").activity[TransformPathsCache]
    transformCache.control.waitUntilFinished()
    transformCache.value().configuredSchema.typedPaths shouldBe empty

    val linkingCache = project.task[LinkSpec]("linking").activity[LinkingPathsCache]
    linkingCache.control.waitUntilFinished()
    // The cached schemas only contain the paths used in the linkage rule
    linkingCache.value().source.typedPaths.map(_.normalizedSerialization) shouldBe IndexedSeq("name")
  }
}
