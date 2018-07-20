package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata._
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsObject, JsValue}

case class EntityMetadataJson(metadata: Map[String, LazyMetadata[_, JsValue]] = Map()) extends EntityMetadata[JsValue]{

  override val serializer: SerializationFormat[EntityMetadata[JsValue], JsValue] =
    EntityMetadataJson.JsonSerializer.asInstanceOf[SerializationFormat[EntityMetadata[JsValue], JsValue]]

  override implicit val serTag: Class[JsValue] = EntityMetadataJson.JsValClass

  override def addReplaceMetadata(key: String, lm: LazyMetadata[_, JsValue]): EntityMetadata[JsValue] =
    EntityMetadataJson((metadata.toSeq.filterNot(_._1 == key) ++ Seq(key -> lm)).toMap)

  override def emptyEntityMetadata: EntityMetadata[JsValue] = EntityMetadataJson()

  override def addFailure(failure: Throwable): EntityMetadata[JsValue] = {
    val lm = IrreplaceableMetadataJson(failure, EntityMetadata.FAILURE_KEY)(classOf[Throwable])
    addReplaceMetadata(EntityMetadata.FAILURE_KEY, lm)
  }
}

object EntityMetadataJson{

  type CT >: Any <: Any

  implicit val JsValClass: Class[JsValue] = classOf[JsValue]

  def apply[Typ](map: Map[String, Typ])(implicit typTag: Class[Typ]): EntityMetadataJson = {
    val resMap = map.map(ent => LazyMetadataJson.createLazyMetadata[Typ](ent._1, ent._2))
    new EntityMetadataJson(resMap)
  }

  def apply(t: Throwable): EntityMetadataJson = apply(Map(EntityMetadata.FAILURE_KEY -> t))(classOf[Throwable])

  def apply(base: EntityMetadata[JsValue]): EntityMetadataJson = EntityMetadataJson(base.metadata)

  def apply(value: String): EntityMetadataJson = apply(JsonSerializer.fromString(value, JsonFormat.MIME_TYPE_APPLICATION)(ReadContext()))

  EntityMetadata.registerNewEntityMetadataFormat(EntityMetadataJson())

  object JsonSerializer extends JsonMetadataSerializer[EntityMetadata[JsValue]] {

    private def extractKey(source: String): String ={
      if(source == null || source.isEmpty){
        ""
      }
      else {
        var keyOpened = false
        val key = for (chr <- source) yield {
          if (keyOpened) {
            if (chr == '"') {
              keyOpened = false
              ""
            }
            else {
              chr.toString
            }
          }
          else {
            if (chr == '"') {
              keyOpened = true
            }
            ""
          }
        }
        key.reduce((a, b) => a + b)
      }
    }

    /**
      * Overriding the read function to circumvent the parsing of the whole metadata collection (parsing the metadata only when needed).
      * Since the json metadata serialization follows a strict pattern (one metadata category per line), we can safely do this.
      * {
      *   "metadataCategory1":{...},
      *   "metadataCategory2":{...},
      *   ...
      * }
      */
    override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): EntityMetadata[JsValue] = {
      if(value == null || value.trim.isEmpty)
        return EntityMetadataJson()
      val lines = value.split("\n")
        .map(_.trim)
        .filter(_.length > 3)
        .map(line => line.splitAt(line.indexOf(':') + 1))
      val map = lines.flatMap(kv => {
        val key = extractKey(kv._1)                                                                               // removing quotes and colon
        var stringRep = kv._2.trim
        stringRep = if(stringRep.last == ',') stringRep.substring(0, stringRep.length - 1) else stringRep         // removing trailing comma
        JsonMetadataSerializer.getSerializationFormat[CT](key).map(serializer => key ->
          LazyMetadataJson[CT](stringRep, serializer)(serializer.valueType.asInstanceOf[Class[CT]]))
      }).toMap
      new EntityMetadataJson(map)
    }


    /**
      * Formats a JSON value as string.
      */
    override def toString(value: EntityMetadata[JsValue], mimeType: String)(implicit writeContext: WriteContext[JsValue]): String = {
      if(value.isEmpty)
        return ""
      val sb = new StringBuilder("{")
      for(ent <- value){
        if(ent != value.head) sb.append(",\n") else sb.append("\n")
        sb.append("\t\"" + ent._1.trim + "\":")
        JsonMetadataSerializer.getSerializationFormat[CT](ent._1) match{
          case Some(serializer) => ent._2.metadata match{
            case Some(m) => sb.append(serializer.toString(m, ent._2.defaultMimeType))
            case None => sb.append("null")
          }
          case None => sb.append("null")
        }
      }
      sb.append("\n}")
      sb.toString()
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): EntityMetadataJson = {
      val map = value.as[JsObject].fields.map(ent => {
        val serializer = JsonMetadataSerializer.getSerializationFormat[CT](ent._1).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + ent._1))
        ent._1 -> LazyMetadataJson[CT](ent._2, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }).toMap
      new EntityMetadataJson(map.asInstanceOf[Map[String, LazyMetadata[_, JsValue]]])
    }

    override def write(em: EntityMetadata[JsValue])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(em.metadata.map(ent => ent._1 -> ent._2.serialized))
    }

    /**
      * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
      * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
      */
    override def metadataId: String = EntityMetadata.METADATA_KEY

    override def replaceableMetadata: Boolean = true    //has no importance for EntityMetadata
  }
}
