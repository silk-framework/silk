package org.silkframework.serialization.json.transformer

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.JsonSerialization
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.{ProjectTask, ProjectTrait}
import play.api.libs.json._

import scala.annotation.tailrec

@Plugin(
  id = "inputTaskAttributes",
  categories = Array("Metadata"),
  label = "Input task attributes",
  description = "Retrieves individual attributes from the input task (such as the modified date) or the entire task as JSON."
)
case class InputTaskAttributesTransformer(@Param("Path to retrieve from the JSON, such as 'metadata/modified'. If left empty, the entire JSON will be returned.")
                                          path: String = "") extends JsonTransformer {

  override def getJson(inputTask: Task[_ <: TaskSpec], project: ProjectTrait)
                      (implicit pluginContext: PluginContext): JsValue = {
      implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(project)(pluginContext.user)
      JsonSerialization.toJson[Task[TaskSpec]](inputTask)
  }
}
