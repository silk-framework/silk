package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.LazyMetadata
import org.silkframework.runtime.serialization.{SerializationFormat, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsObject, JsValue}

import scala.reflect.ClassTag

case class LazyMetadataJson[Typ] private(
   obj: Option[Typ],
   serial: Option[JsValue],
   string: String,
   serializer: SerializationFormat[Typ, JsValue]
 )(implicit val typ: Class[Typ]) extends LazyMetadata[Typ, JsValue] {

  assert(obj.nonEmpty || serial.nonEmpty, "LazyMetadata without any data object.")

  override implicit val serTag: ClassTag[JsValue] = ClassTag(classOf[JsValue])
  override implicit val typTag: ClassTag[Typ] = ClassTag(typ)
  private implicit val nodeWc = WriteContext[JsValue]()
  /**
    * the schema object defining the parsed metadata object
    */
  override val schema: LazyMetadata.Schema = None

  /**
    * the final metadata object lazily computed
    */
  override lazy val metadata: Option[Typ] = obj match{
    case Some(x) => Some(x)
    case None => Option(serializer.read(serialized))
  }

  /**
    * the raw, un-parsed metadata
    */
  override def serialized: JsValue = serial match{
    case Some(s) => s
    case None => string match{
      case s: String if s.trim.nonEmpty => serializer.parse(s, JsonFormat.MIME_TYPE_APPLICATION)
      case _ => obj match{
        case Some(x) => serializer.write(x)
        case None => JsObject(Seq())
      }
    }
  }
}

object LazyMetadataJson{
  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): LazyMetadataJson[Typ] = apply(Some(obj), None, "", serializer)(typ)

  def apply[Typ](node: JsValue, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): LazyMetadataJson[Typ] = apply(None, Some(node), "", serializer)(typ)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): LazyMetadataJson[Typ] = apply(None, None, ser, serializer)(typ)
}
