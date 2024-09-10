package org.silkframework.serialization.json

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.JsValue

@Plugin(
  id = "taskJson",
  categories = Array("Dataset"),
  label = "Task JSON",
  description = "Retrieves the input task as JSON."
)
case class TaskJsonTransformer() extends Transformer {

  override def withContext(taskContext: TaskContext): Transformer = {
    taskContext.inputTasks.headOption match {
      case Some(inputTask) =>
        val project = inputTask match {
          case projectTask: ProjectTask[_] =>
            projectTask.project
          case _ =>
            throw new ValidationException("This transform can only be executed with a project task.")
        }
        implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(project)(taskContext.userContext)
        val taskJson = JsonSerialization.toJson[Task[TaskSpec]](inputTask.asInstanceOf[Task[TaskSpec]])
        new PropertyRetriever(taskJson)
      case None =>
        this
    }
  }

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    throw new ValidationException("No input task available.")
  }

  private class PropertyRetriever(json: JsValue) extends Transformer {

    def apply(values: Seq[Seq[String]]): Seq[String] = {
      Seq(json.toString())
    }

  }

}



