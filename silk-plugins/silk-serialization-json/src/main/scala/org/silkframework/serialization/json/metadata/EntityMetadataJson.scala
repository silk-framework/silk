package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{EntityMetadata, LazyMetadata}
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsObject, JsValue}

import scala.collection.mutable

case class EntityMetadataJson(metadata: Map[String, LazyMetadata[_, JsValue]]) extends EntityMetadata[JsValue]{

  override val serializer: SerializationFormat[EntityMetadata[JsValue], JsValue] =
    EntityMetadataJson.JsonSerializer.asInstanceOf[SerializationFormat[EntityMetadata[JsValue], JsValue]]

  override implicit val serTag: Class[JsValue] = classOf[JsValue]
}

object EntityMetadataJson{

  type CT >: Any <: Any

  object JsonSerializer extends JsonFormat[EntityMetadataJson] {

    /**
      * Overriding the read function to circumvent the parsing of the whole metadata collection (parsing the metadata only when needed).
      * Since the json metadata serialization follows a strict pattern (one metadata category per line), we can safely do this.
      * {
      *   "metadataCategory1":{...},
      *   "metadataCategory2":{...},
      *   ...
      * }
      */
    override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): EntityMetadataJson = {
      val lines = value.split("\n").map(_.trim).filter(_.length > 3).map(line => line.splitAt(line.indexOf(':')))
      val map = lines.map(kv => {
        val key = kv._1.replace("\"", "")                                         // removing quotes
        var stringRep = kv._2.trim
        stringRep = if(stringRep.last == ',') stringRep.substring(0, stringRep.length - 1) else stringRep    // removing trailing comma
        val serializer = JsonMetadataSerializer.getSerializationFormat[CT](key).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + key))
        key -> LazyMetadataJson[CT](stringRep, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }).toMap
      new EntityMetadataJson(map)
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): EntityMetadataJson = {
      val map = value.as[JsObject].fields.map(ent => {
        val serializer = JsonMetadataSerializer.getSerializationFormat[CT](ent._1).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + ent._1))
        ent._1 -> LazyMetadataJson[CT](ent._2, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }).toMap
      new EntityMetadataJson(map.asInstanceOf[Map[String, LazyMetadata[_, JsValue]]])
    }

    override def write(em: EntityMetadataJson)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(em.metadata.map(ent => ent._1 -> ent._2.serialized))
    }
  }
}
