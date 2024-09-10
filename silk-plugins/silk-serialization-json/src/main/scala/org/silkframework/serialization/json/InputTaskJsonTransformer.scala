package org.silkframework.serialization.json

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{JsBoolean, JsDefined, JsLookupResult, JsNumber, JsString, JsUndefined, JsValue}

import scala.annotation.tailrec

@Plugin(
  id = "inputTaskJson",
  categories = Array("Dataset"),
  label = "Input Task JSON",
  description = "Retrieves the input task or properties of it as JSON."
)
case class InputTaskJsonTransformer(@Param("Path to retrieve from the JSON, such as 'metadata/modified'. If left empty, the entire JSON will be returned.")
                                    path: String = "") extends Transformer {

  override def withContext(taskContext: TaskContext): Transformer = {
    taskContext.inputTasks.headOption match {
      case Some(inputTask) =>
        val project = inputTask match {
          case projectTask: ProjectTask[_] =>
            projectTask.project
          case _ =>
            throw new ValidationException("This transform can only be executed with a project task.")
        }
        implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(project)(taskContext.pluginContext.user)
        val taskJson = JsonSerialization.toJson[Task[TaskSpec]](inputTask)
        navigatePath(taskJson, path) match {
          case JsDefined(value) =>
            ConstantTransformer(stringValue(value))
          case undefined: JsUndefined =>
            throw new IllegalArgumentException(undefined.error)
        }
      case None =>
        this
    }
  }

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    throw new ValidationException("No input task available.")
  }

  @tailrec
  private def navigatePath(json: JsValue, path: String): JsLookupResult = {
    val parts = path.split('/')
    if(path.isEmpty || parts.isEmpty) {
      JsDefined(json)
    } else {
      json \ parts.head match {
        case JsDefined(value) =>
          navigatePath(value, parts.tail.mkString("/"))
        case undefined: JsUndefined =>
          undefined
      }
    }
  }

  private def stringValue(json: JsValue): String = {
    json match {
      case JsString(value) =>
        value
      case JsBoolean(value) =>
        value.toString
      case JsNumber(value) =>
        value.toString()
      case _ =>
        json.toString()
    }
  }

}
