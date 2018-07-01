package org.silkframework.serialization.json

import org.silkframework.entity.metadata.{EntityMetadata, LazyMetadata}
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
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
    override def read(value: JsValue)(implicit readContext: ReadContext): EntityMetadataJson = {
      val map = value.as[JsObject].fields.map(ent => {
        val serializer = EntityMetadataJson.getSerializationFormat[CT](ent._1).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + ent._1))
        ent._1 -> LazyMetadataJson[CT](ent._1, ent._2, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }).toMap
      new EntityMetadataJson(map.asInstanceOf[Map[String, LazyMetadata[_, JsValue]]])
    }

    override def write(em: EntityMetadataJson)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(em.metadata.map(ent => ent._1 -> ent._2.serialized))
    }
  }

  /* Serializer Registry */

  private val SerializerRegistry = new mutable.HashMap[String, JsonFormat[_]]

  def registerSerializationFormat(key: String, sf: JsonFormat[_]): Unit ={
    //add entry for each mime type
    SerializerRegistry.put(key, sf)
  }

  def getSerializationFormat[T](key: String): Option[JsonFormat[T]] ={
    SerializerRegistry.get(key).flatMap{
      case jf: JsonFormat[_] => Some(jf.asInstanceOf[JsonFormat[T]])
      case _ => None
    }
  }

  def listAllSerializers: List[JsonFormat[_]] = SerializerRegistry.values.toList.distinct
}
