package controllers.workspaceApi.activities

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.ActivitySerializers.StatusJsonFormat
import play.api.libs.json.{Format, JsResult, JsSuccess, JsValue, Json}

/**
  * The status of a single activity.
  */
case class TaskActivityStatus(projectId: String,
                              taskId: String,
                              activityId: String,
                              startTime: Option[Long],
                              concreteStatus: String,
                              statusDetails: Status)

object TaskActivityStatus {
  implicit val statusFormat: Format[Status] = new Format[Status] {
    implicit val readContext: ReadContext = ReadContext()
    implicit val writeContext: WriteContext[JsValue] = WriteContext()
    override def reads(json: JsValue): JsResult[Status] = {
      JsSuccess(StatusJsonFormat.read(json))
    }

    override def writes(status: Status): JsValue = {
      StatusJsonFormat.write(status)
    }
  }
  implicit val taskActivityStatusFormat: Format[TaskActivityStatus] = Json.format[TaskActivityStatus]
}
