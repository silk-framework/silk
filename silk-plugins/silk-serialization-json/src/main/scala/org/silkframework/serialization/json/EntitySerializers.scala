package org.silkframework.serialization.json

import org.silkframework.execution.EntityHolder
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.{JsArray, JsString, JsValue, Json}

object EntitySerializers {

  implicit object EntityHolderJsonFormat extends JsonFormat[EntityHolder] {

    override def read(value: JsValue)(implicit readContext: ReadContext): EntityHolder = {
      throw new UnsupportedOperationException("Parsing EntityHolders from Json is not supported at the moment")
    }

    override def write(value: EntityHolder)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      // Append header
      val header: Array[String] = value.entitySchema.typedPaths.map(path => path.serialize(stripForwardSlash = true)(writeContext.prefixes)).toArray

      // Convert entity values to a nested JSON array
      val valuesJson = JsArray(
        for(entity <- value.entities.toSeq) yield {
          val values = entity.values.map(values => JsArray(values.map(JsString)))
          JsArray(values)
        }
      )

      Json.obj("taskLabel" -> value.taskLabel,"attributes" -> header, "values" -> valuesJson)
    }
  }
}
