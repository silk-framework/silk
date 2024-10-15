package org.silkframework.serialization.json.transformer

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.{ProjectTask, ProjectTrait, WorkspaceFactory}
import play.api.libs.json._

import scala.annotation.tailrec

/**
 * Transformer that retrieves JSON and allows to filter on it.
 */
trait JsonTransformer extends Transformer {

  /**
   * Path to retrieve from the JSON. If left empty, the entire JSON will be returned
   */
  def path: String

  /**
   * Retrieves the JSON.
   */
  def getJson(inputTask: Task[_ <: TaskSpec], project: ProjectTrait)
             (implicit pluginContext: PluginContext): JsValue

  override def withContext(taskContext: TaskContext): Transformer = {
    val inputTask = taskContext.inputTasks.headOption.getOrElse(throw new ValidationException("This task does not have an input"))
    val project = taskContext.pluginContext.projectId match {
      case Some(projectId) =>
        implicit val user: UserContext = taskContext.pluginContext.user
        WorkspaceFactory().workspace.project(projectId)
      case _ =>
        throw new ValidationException("Needs to be executed in a project context")
    }
    val json = getJson(inputTask, project)(taskContext.pluginContext)

    navigatePath(json, path) match {
      case JsDefined(value) =>
        ConstantTransformer(stringValue(value))
      case undefined: JsUndefined =>
        throw new IllegalArgumentException(undefined.error)
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
        Json.prettyPrint(json)
    }
  }
}
