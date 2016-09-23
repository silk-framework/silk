package org.silkframework.serialization.json

import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json._

/**
  * Serializers for JSON.
  */
object JsonSerializers {

  implicit object JsonDatasetTaskFormat extends JsonFormat[DatasetTask] {

    override def read(value: JsValue)(implicit readContext: ReadContext): DatasetTask = {
      implicit val prefixes = readContext.prefixes
      implicit val resource = readContext.resources
      new DatasetTask(
        id = (value \ "id").as[JsString].value,
        plugin =
          Dataset(
            id = (value \ "type").as[JsString].value,
            params = (value \ "parameters").as[JsObject].value.mapValues(_.as[JsString].value).asInstanceOf[Map[String, String]]
          )
        )
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
