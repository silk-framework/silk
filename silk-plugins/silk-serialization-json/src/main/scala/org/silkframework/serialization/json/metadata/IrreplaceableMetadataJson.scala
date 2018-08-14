package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.IrreplaceableMetadata
import org.silkframework.runtime.serialization.SerializationFormat
import play.api.libs.json.JsValue

class IrreplaceableMetadataJson[Typ] private[metadata](
    obj: Option[Typ],
    serial: Option[JsValue],
    string: String,
    serializer: SerializationFormat[Typ, JsValue]
  )(implicit override val typ: Class[Typ]) extends LazyMetadataJson[Typ](obj, serial, string, serializer) with IrreplaceableMetadata

object IrreplaceableMetadataJson{

  def createLazyMetadata[Typ](key: String, t: Typ)(implicit typTag: Class[Typ]): (String, IrreplaceableMetadataJson[Typ]) = {
    val serializer = JsonMetadataSerializer.getSerializer[Typ](key)
    (key, IrreplaceableMetadataJson(t, serializer))
  }

  def apply[Typ](lm: LazyMetadataJson[Typ])(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    new IrreplaceableMetadataJson[Typ](lm.obj, lm.serial, lm.str, lm.serializer)

  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    new IrreplaceableMetadataJson[Typ](Option(obj), None, "", serializer)(typ)

  def apply[Typ](node: JsValue, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    new IrreplaceableMetadataJson[Typ](None, Option(node), "", serializer)(typ)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, JsValue])(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    new IrreplaceableMetadataJson[Typ](None, None, ser, serializer)(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](obj: Typ, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    apply(obj, JsonMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](node: JsValue, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    apply(node, JsonMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](ser: String, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataJson[Typ] =
    apply(ser, JsonMetadataSerializer.getSerializer[Typ](serializer))(typ)
}
