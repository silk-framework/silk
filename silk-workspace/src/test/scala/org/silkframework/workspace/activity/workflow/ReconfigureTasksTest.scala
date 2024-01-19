package org.silkframework.workspace.activity.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.types.IdentifierOptionParameter
import org.silkframework.workspace.activity.workflow.ReconfigureTasks.ReconfigurableTask

class ReconfigureTasksTest extends AnyFlatSpec with Matchers {

  behavior of "ReconfigureTasks"

  private implicit val pluginContext: PluginContext = PluginContext.empty

  it should "reconfigure transform tasks" in {
    val transform = PlainTask("transform-task", TransformSpec(DatasetSelection("original-source"), output = IdentifierOptionParameter(Some("original-output"))))
    val updatedTransform = reconfigure(transform, Map("selection-inputId" -> "replaced-source", "output" -> "replaced-output"))
    updatedTransform.data.selection.inputId shouldBe "replaced-source"
    updatedTransform.data.output shouldBe IdentifierOptionParameter(Some("replaced-output"))
  }

  it should "reconfigure linking tasks" in {
    val linkSpec = PlainTask("linking-task", LinkSpec(DatasetSelection("original-source"), DatasetSelection("original-target"), output = IdentifierOptionParameter(Some("original-output"))))
    val updatedLinkSpec = reconfigure(linkSpec, Map("source-inputId" -> "replaced-source", "target-inputId" -> "replaced-target", "output" -> "replaced-output"))
    updatedLinkSpec.data.source.inputId shouldBe "replaced-source"
    updatedLinkSpec.data.target.inputId shouldBe "replaced-target"
    updatedLinkSpec.data.output shouldBe IdentifierOptionParameter(Some("replaced-output"))
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
    task.reconfigure(Seq(entity))
  }

}
