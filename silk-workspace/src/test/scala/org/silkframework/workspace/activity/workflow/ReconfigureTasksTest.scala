package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.plugins.dataset.text.TextFileDataset
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.types.IdentifierOptionParameter
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.activity.workflow.ReconfigureTasks.ReconfigurableTask
import org.silkframework.workspace.resources.ConstantResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, Workspace}

class ReconfigureTasksTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "ReconfigureTasks"

  private implicit val userContext: UserContext = UserContext.Empty
  private val resources = InMemoryResourceManager()
  private implicit val pluginContext: PluginContext = PluginContext(Prefixes.default, resources, taskResolver = TaskResolver.empty)
  private val project = new Workspace(new InMemoryWorkspaceProvider(), ConstantResourceRepository(resources)).createProject(ProjectConfig("reconfigure-tasks"))

  it should "reconfigure transform tasks" in {
    val transform = PlainTask("transform-task", TransformSpec(DatasetSelection("original-source"), output = IdentifierOptionParameter(Some("original-output"))))
    val updatedTransform = reconfigure(transform, Map("selection-inputId" -> "replaced-source", "output" -> "replaced-output"))
    updatedTransform.data.selection.inputId shouldBe IdentifierOptionParameter(Some("replaced-source"))
    updatedTransform.data.output shouldBe IdentifierOptionParameter(Some("replaced-output"))
  }

  it should "reconfigure linking tasks" in {
    val linkSpec = PlainTask("linking-task", LinkSpec(DatasetSelection("original-source"), DatasetSelection("original-target"), output = IdentifierOptionParameter(Some("original-output"))))
    val updatedLinkSpec = reconfigure(linkSpec, Map("source-inputId" -> "replaced-source", "target-inputId" -> "replaced-target", "output" -> "replaced-output"))
    updatedLinkSpec.data.source.inputId shouldBe IdentifierOptionParameter(Some("replaced-source"))
    updatedLinkSpec.data.target.inputId shouldBe IdentifierOptionParameter(Some("replaced-target"))
    updatedLinkSpec.data.output shouldBe IdentifierOptionParameter(Some("replaced-output"))
  }

  it should "overwrite reconfigured values even if a template is defined for them" in {
    val dataset = PluginRegistry.create[Dataset]("text", ParameterValues(Map("file" -> ParameterTemplateValue("template"))))
    val updatedDataset = reconfigure(PlainTask("test", DatasetSpec(dataset)), Map("file" -> "updated"))

    dataset.parameters.values.get("file") shouldBe Some(ParameterTemplateValue("template"))
    updatedDataset.parameters.values.get("file") shouldBe Some(ParameterStringValue("updated"))
  }

  it should "resolve templates of parameters that are not overwritten against the task's own execution variables" in {
    val fileVariable = TemplateVariables(Seq(TemplateVariable("file", "default.txt", scope = VariableScope.execution)))
    val dataset = {
      val projectContext = PluginContext.fromProject(project)
      implicit val pluginContext: PluginContext = projectContext.copy(templateVariables = projectContext.templateVariables.withExecutionDefaults(fileVariable))
      PluginRegistry.create[Dataset]("text", ParameterValues(Map("file" -> ParameterTemplateValue("{{execution.file}}"))))
    }
    val task = PlainTask("test", DatasetSpec(dataset), executionVariables = fileVariable)

    val updatedDataset = reconfigure(task, Map("charset" -> "ISO-8859-1")).data.plugin.asInstanceOf[TextFileDataset]

    updatedDataset.file.name shouldBe "default.txt"
    updatedDataset.charset shouldBe "ISO-8859-1"
  }

  private def reconfigure[T <: TaskSpec](task: Task[T], values: Map[String, String]): Task[T] = {
    val orderedValues = values.toIndexedSeq
    val entity =
      Entity(
        uri = "testEntity",
        values = orderedValues.map(v => Seq(v._2)),
        schema =
          EntitySchema(
            typeUri = "testType",
            typedPaths = orderedValues.map(v => UntypedPath.parse(v._1).asStringTypedPath)
          )
      )
    task.reconfigure(Seq(entity), project)
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some("simpleSubstitution")
  )
}
