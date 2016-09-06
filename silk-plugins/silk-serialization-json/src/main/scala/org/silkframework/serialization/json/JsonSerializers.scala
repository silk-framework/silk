package org.silkframework.serialization.json

import org.silkframework.dataset.DatasetTask
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

/**
  * Serializers for JSON.
  */
object JsonSerializers {

  implicit object JsonDatasetTaskFormat extends JsonFormat[DatasetTask] {

    override def read(value: JsValue)(implicit readContext: ReadContext): DatasetTask = {
      ???
    }

    override def write(value: DatasetTask)(implicit writeContext: WriteContext[JsValue]) = {
      JsObject(
        "id" -> JsString(value.id.toString) ::
        "type" -> JsString(value.plugin.plugin.id.toString) ::
        "parameters" -> Json.toJson(value.plugin.parameters) :: Nil
      )

    }

  }

}
