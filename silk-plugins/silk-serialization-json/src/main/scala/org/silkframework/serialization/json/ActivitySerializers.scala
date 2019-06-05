package org.silkframework.serialization.json

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.serialization.WriteContext
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

}
