package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, Restriction, StringValueType}
import org.silkframework.execution.EntityHolder
import org.silkframework.serialization.json.JsonHelpers.{mustBeDefined, mustBeJsArray, numberValueOption, optionalValue, requiredValue, stringValue}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.activity.transform.CachedEntitySchemata
import play.api.libs.json.{JsArray, JsString, JsValue, Json}

object EntitySerializers {

  // TODO: Typed paths are not correctly serialized here, check where this serialization is consumed
  implicit object EntitySchemaJsonFormat extends JsonFormat[EntitySchema] {

    override def read(value: JsValue)(implicit readContext: ReadContext): EntitySchema = {
      implicit val prefixes: Prefixes = readContext.prefixes
      EntitySchema(
        typeUri = new Uri(stringValue(value, "typeUri")),
        typedPaths = mustBeJsArray(requiredValue(value, "paths"))(_.value.map(pathStr => TypedPath(pathStr.as[String], StringValueType))),
        filter = Restriction.parse(stringValue(value, "filter")),
        subPath = UntypedPath.parse(stringValue(value, "subPath"))
      )
    }

    override def write(value: EntitySchema)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      implicit val prefixes: Prefixes = writeContext.prefixes
      val paths = for(typedPath <- value.typedPaths) yield JsString(typedPath.toUntypedPath.serialize())
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

  implicit object CachedEntitySchemataJsonFormat extends WriteOnlyJsonFormat[CachedEntitySchemata] {

    override def write(value: CachedEntitySchemata)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "configured" -> EntitySchemaJsonFormat.write(value.configuredSchema),
        "untyped" -> value.untypedSchema.map(EntitySchemaJsonFormat.write)
      )
    }
  }

  implicit object EntityHolderJsonFormat extends WriteOnlyJsonFormat[EntityHolder] {

    override def write(value: EntityHolder)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      // Append header
      val header: Array[String] = value.entitySchema.typedPaths.map(path => path.toUntypedPath.serialize()(writeContext.prefixes)).toArray

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

  class EntityJsonFormat(includeSchema: Boolean = true) extends JsonFormat[Entity] {

    override def read(value: JsValue)(implicit readContext: ReadContext): Entity = {
      Entity(
        uri = stringValue(value, "uri"),
        values = mustBeJsArray(requiredValue(value, "values"))(value => value.value.map(v => mustBeJsArray(v)(innerValue => innerValue.value.map(_.as[String])))),
        schema = EntitySchemaJsonFormat.read(requiredValue(value, "schema"))
      )
    }

    override def write(entity: Entity)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val entityJson =
        Json.obj(
          "uri" -> entity.uri.toString,
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
