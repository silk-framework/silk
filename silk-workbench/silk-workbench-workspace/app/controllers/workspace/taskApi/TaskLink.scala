package controllers.workspace.taskApi

import play.api.libs.json.{Format, Json}

/** All data needed to link to a task in the workbench UI. */
case class TaskLink(id: String, label: Option[String], taskLink: String)

object TaskLink {
  implicit val taskLinkFormat: Format[TaskLink] = Json.format[TaskLink]
}
