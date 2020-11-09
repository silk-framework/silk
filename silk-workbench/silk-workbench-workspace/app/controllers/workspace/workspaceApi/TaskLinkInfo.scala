package controllers.workspace.workspaceApi

import play.api.libs.json.{Format, Json}

/**
  * All information necessary to link a task.
  */
case class TaskLinkInfo(id: String, label: String, taskType: Option[String])

object TaskLinkInfo {
  implicit val taskLinkInfoFormat: Format[TaskLinkInfo] = Json.format[TaskLinkInfo]
}
