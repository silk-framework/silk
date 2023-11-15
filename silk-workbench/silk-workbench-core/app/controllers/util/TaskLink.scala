package controllers.util

import config.WorkbenchLinks
import org.silkframework.config.TaskSpec
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{Format, Json}

/** All data needed to link to a task in the workbench UI. */
case class TaskLink(id: String, label: Option[String], taskLink: String)

object TaskLink {

  def fromTask(task: ProjectTask[_ <: TaskSpec]): TaskLink = {
    TaskLink(task.id, task.metaData.label, WorkbenchLinks.editorLink(task))
  }

  implicit val taskLinkFormat: Format[TaskLink] = Json.format[TaskLink]
}
