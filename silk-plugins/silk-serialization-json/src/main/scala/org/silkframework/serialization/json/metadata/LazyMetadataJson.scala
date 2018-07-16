package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{LazyMetadata, UnreplaceableMetadata}
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.{JsObject, JsValue}

import scala.reflect.ClassTag

case class LazyMetadataJson[Typ] private(
   obj: Option[Typ],
   serial: Option[JsValue],
   string: String,
   serializer: SerializationFormat[Typ, JsValue]
 )(implicit val typ: Class[Typ]) extends LazyMetadata[Typ, JsValue] {

  assert(obj.nonEmpty || serial.nonEmpty || string.nonEmpty, "LazyMetadata without any data object.")

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
    case None => Option(serializer.read(serialized)(ReadContext()))
  }

  /**
    * the raw, un-parsed metadata
    */
  override def serialized: JsValue = serial match{
    case Some(s) => s
    case None => string match{
      case s: String if s.trim.nonEmpty => serializer.parse(s, defaultMimeType)
      case _ => obj match{
        case Some(x) => serializer.write(x)
        case None => JsObject(Seq())
      }
    }
  }

  /**
    * Providing the default mime type to be used with the serializer
    */
  override val defaultMimeType: String = JsonFormat.MIME_TYPE_APPLICATION
}

private class UnreplaceableLazyMetadataJson[Typ](
   obj: Option[Typ],
   serial: Option[JsValue],
   string: String,
   serializer: SerializationFormat[Typ, JsValue]
)(implicit override val typ: Class[Typ]) extends LazyMetadataJson[Typ](obj, serial, string, serializer) with UnreplaceableMetadata[Typ, JsValue]

object LazyMetadataJson{

  def createLazyMetadata[Typ](key: String, t: Typ, isReplaceable: Boolean = true)(implicit typTag: Class[Typ]): Option[(String, LazyMetadataJson[Typ])] = {
    JsonMetadataSerializer.getSerializationFormat[Typ](key).map(serializer => (key, LazyMetadataJson(t, serializer, isReplaceable)))
  }
  //TODO mak serializer a broadcast, dont serialize it with the metadata
  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, JsValue], isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    if(isReplaceable) apply(Option(obj), None, "", serializer)(typ) else new UnreplaceableLazyMetadataJson[Typ](Option(obj), None, "", serializer)

  def apply[Typ](node: JsValue, serializer: SerializationFormat[Typ, JsValue], isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    if(isReplaceable) apply(None, Option(node), "", serializer)(typ) else new UnreplaceableLazyMetadataJson[Typ](None, Option(node), "", serializer)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, JsValue], isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    if(isReplaceable) apply(None, None, ser, serializer)(typ) else new UnreplaceableLazyMetadataJson[Typ](None, None, ser, serializer)

  @throws[IllegalArgumentException]
  def apply[Typ](obj: Typ, serializer: String, isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    apply(obj, JsonMetadataSerializer.getSerializer[Typ](serializer), isReplaceable)(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](node: JsValue, serializer: String, isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    apply(node, JsonMetadataSerializer.getSerializer[Typ](serializer), isReplaceable)(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](ser: String, serializer: String, isReplaceable: Boolean)(implicit typ: Class[Typ]): LazyMetadataJson[Typ] =
    apply(ser, JsonMetadataSerializer.getSerializer[Typ](serializer), isReplaceable)(typ)
}
