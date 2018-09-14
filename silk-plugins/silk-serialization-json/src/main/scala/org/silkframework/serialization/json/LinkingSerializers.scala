package org.silkframework.serialization.json

import org.silkframework.entity.Link
import org.silkframework.rule.execution.Linking
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.EntitySerializers.{EntityJsonFormat, PairEntitySchemaJsonFormat}
import org.silkframework.serialization.json.JsonSerializers.PairJsonFormat
import play.api.libs.json.{JsValue, Json}

object LinkingSerializers {

  implicit object LinkingJsonFormat extends WriteOnlyJsonFormat[Linking] {
    final val LINKS = "links"
    final val STATISTICS = "statistics"
    final val ENTITY_SCHEMATA = "entitySchemata"

    override def write(value: Linking)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val firstEntityOption = value.links.headOption.flatMap(_.entities)
      val entitySchemataOption = firstEntityOption.map(_.map(_.schema))

      Json.obj(
        LINKS -> value.links.map(LinkJsonFormat.write),
        STATISTICS -> Json.obj(
          "sourceEntities" -> value.statistics.entityCount.source,
          "targetEntities" -> value.statistics.entityCount.target,
          "linkCount" -> value.links.size
        ),
        ENTITY_SCHEMATA -> entitySchemataOption.map(PairEntitySchemaJsonFormat.write)
      )
    }
  }

  implicit object LinkJsonFormat extends WriteOnlyJsonFormat[Link] {
    final val SOURCE = "source"
    final val TARGET = "target"
    final val CONFIDENCE = "confidence"
    final val ENTITIES = "entities"

    private val entityPairFormat = new PairJsonFormat()(new EntityJsonFormat(includeSchema = false))

    override def write(value: Link)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        SOURCE -> value.source,
        TARGET -> value.target,
        CONFIDENCE -> value.confidence,
        ENTITIES -> value.entities.map(entityPairFormat.write)
      )
    }
  }

}
