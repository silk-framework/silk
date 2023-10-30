package controllers.workspaceApi.projectTask

import controllers.workspaceApi.project.ProjectApiRestPayloads.ItemMetaData
import play.api.libs.json.{Format, Json}

/**
  * A request to clone a task.
  */
case class ItemCloneRequest(metaData: ItemMetaData, newTaskId: Option[String])

object ItemCloneRequest {
  implicit val taskCloneRequestFormat: Format[ItemCloneRequest] = Json.format[ItemCloneRequest]
}

case class ItemCloneResponse(id: String, detailsPage: String)

object ItemCloneResponse {
  implicit val taskCloneResponseFormat: Format[ItemCloneResponse] = Json.format[ItemCloneResponse]
}
