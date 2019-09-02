package org.silkframework.serialization.json

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Idle, Running, Waiting}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.workspace.activity.WorkspaceActivity
import play.api.libs.json._

object ActivitySerializers {

  implicit object StatusJsonFormat extends JsonFormat[Status] {

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val basicParameters =
        JsObject(
          ("statusName" -> JsString(status.name)) ::
          ("isRunning" -> JsBoolean(status.isRunning)) ::
          ("progress" -> status.progress.map(p => JsNumber(p * 100.0)).getOrElse(JsNull)) ::
          ("message" -> JsString(status.toString)) ::
          ("failed" -> JsBoolean(status.failed)) ::
          ("lastUpdateTime" -> JsNumber(status.timestamp)) :: Nil
        )
      status match {
        case Finished(success, runtime, cancelled, exception) =>
          basicParameters +
            ("runtime" -> JsNumber(runtime)) +
            ("cancelled" -> JsBoolean(cancelled)) +
            ("exceptionMessage" -> exception.map(_.getMessage).map(JsString).getOrElse(JsNull))
        case _ =>
          basicParameters
      }
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): Status = {
      (value \ "statusName").as[String] match {
        case "Idle" =>
          Idle()
        case "Waiting" =>
          Waiting()
        case "Running" =>
          Running(
            message = (value \ "message").as[String],
            progress = (value \ "progress").toOption.map(_.as[Double])
          )
        case "Canceling" =>
          Canceling(
            progress = (value \ "progress").toOption.map(_.as[Double])
          )
        case "Finished" =>
          Finished(
            success = !(value \ "failed").as[Boolean],
            runtime = (value \ "runtime").as[Long],
            cancelled = (value \ "cancelled").as[Boolean],
            // At the moment the stack trace gets lost during serialization:
            exception = (value \ "exceptionMessage").asOpt[String].map(msg => new Exception(msg))
          )
      }
    }
  }

  class ExtendedStatusJsonFormat(project: String, task: String, activity: String, startTime: Option[Long]) extends WriteOnlyJsonFormat[Status] {

    def this(activity: WorkspaceActivity[_]) = {
      this(activity.project.name, activity.taskOption.map(_.id.toString).getOrElse(""), activity.name, activity.startTime)
    }

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      StatusJsonFormat.write(status).as[JsObject] +
      ("project" -> JsString(project)) +
      ("task" -> JsString(task)) +
      ("activity" -> JsString(activity)) +
      ("startTime" -> startTime.map(JsNumber(_)).getOrElse(JsNull))
    }
  }

}
