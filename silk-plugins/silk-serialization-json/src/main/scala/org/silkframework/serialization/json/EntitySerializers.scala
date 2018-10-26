package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.util.DPair
import play.api.libs.json.{JsArray, JsString, JsValue, Json}

object EntitySerializers {

  implicit object EntitySchemaJsonFormat extends WriteOnlyJsonFormat[EntitySchema] {

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

  implicit object PairEntitySchemaJsonFormat extends WriteOnlyJsonFormat[DPair[EntitySchema]] {

    override def write(value: DPair[EntitySchema])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "source" -> EntitySchemaJsonFormat.write(value.source),
        "target" -> EntitySchemaJsonFormat.write(value.target)
      )
    }
  }

  implicit object EntityHolderJsonFormat extends WriteOnlyJsonFormat[EntityHolder] {

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

  class EntityJsonFormat(includeSchema: Boolean = true) extends WriteOnlyJsonFormat[Entity] {

    override def write(entity: Entity)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val entityJson =
        Json.obj(
          "values" -> entity.values.map(values => JsArray(values.map(JsString)))
        )

      if(includeSchema) {
        entityJson ++ Json.obj("schema" -> EntitySchemaJsonFormat.write(entity.schema))
      } else {
        entityJson
      }
    }
  }
}
