package controllers.workspaceApi.projectTask

import controllers.workspaceApi.project.ProjectApiRestPayloads.ItemMetaData
import play.api.libs.json.{Format, Json}

/**
  * A request to clone a task.
  */
case class TaskCloneRequest(metaData: ItemMetaData)

object TaskCloneRequest {
  implicit val taskCloneRequestFormat: Format[TaskCloneRequest] = Json.format[TaskCloneRequest]
}

case class TaskCloneResponse(id: String, taskLink: Option[String])

object TaskCloneResponse {
  implicit val taskCloneResponseFormat: Format[TaskCloneResponse] = Json.format[TaskCloneResponse]
}
