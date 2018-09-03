package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.entity.{EntitySchema, Path, Restriction}
import org.silkframework.execution.EntityHolder
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.DPair
import play.api.libs.json.{JsArray, JsString, JsValue, Json}

object EntitySerializers {

  implicit object EntitySchemaJsonFormat extends JsonFormat[EntitySchema] {

    override def read(value: JsValue)(implicit readContext: ReadContext): EntitySchema = {
      throw new UnsupportedOperationException("Parsing EntitySchema from Json is not supported at the moment")
    }

    override def write(value: EntitySchema)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      implicit val prefixes: Prefixes = writeContext.prefixes
      val paths = for(typedPath <- value.typedPaths) yield JsString(typedPath.serialize())
      Json.obj(
        "typeUri" -> value.typeUri.uri,
        "paths" -> JsArray(paths),
        "filter" -> value.filter.serialize,
        "subPath" -> value.subPath.serialize()
      )
    }
  }

  implicit object PairEntitySchemaJsonFormat extends JsonFormat[DPair[EntitySchema]] {

    override def read(value: JsValue)(implicit readContext: ReadContext): DPair[EntitySchema] = {
      throw new UnsupportedOperationException("Parsing DPair[EntitySchema] from Json is not supported at the moment")
    }

    override def write(value: DPair[EntitySchema])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "source" -> EntitySchemaJsonFormat.write(value.source),
        "target" -> EntitySchemaJsonFormat.write(value.target)
      )
    }
  }

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
