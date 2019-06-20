package org.silkframework.serialization.json

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.workspace.activity.WorkspaceActivity
import play.api.libs.json._

object ActivitySerializers {

  implicit object StatusJsonFormat extends WriteOnlyJsonFormat[Status] {

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        ("statusName" -> JsString(status.name)) ::
        ("isRunning" -> JsBoolean(status.isRunning)) ::
        ("progress" -> status.progress.map(p => JsNumber(p * 100.0)).getOrElse(JsNull)) ::
        ("message" -> JsString(status.toString)) ::
        ("failed" -> JsBoolean(status.failed)) ::
        ("lastUpdateTime" -> JsNumber(status.timestamp)) :: Nil
      )
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
