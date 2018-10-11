package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.SerializationFormat

import scala.xml.Node

class IrreplaceableMetadataXml[Typ] private[metadata](
   obj: Option[Typ],
   serial: Option[Node],
   string: String,
   serializer: SerializationFormat[Typ, Node] with MetadataSerializer
 )(implicit override val typ: Class[Typ]) extends LazyMetadataXml[Typ](obj, serial, string, serializer) with IrreplaceableMetadata

object IrreplaceableMetadataXml{

  def apply[Typ](lm: LazyMetadataXml[Typ])(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    new IrreplaceableMetadataXml[Typ](lm.obj, lm.serial, lm.str, lm.serializer)

  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    new IrreplaceableMetadataXml[Typ](Option(obj), None, "", serializer)(typ)

  def apply[Typ](node: Node, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    new IrreplaceableMetadataXml[Typ](None, Option(node), "", serializer)(typ)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    new IrreplaceableMetadataXml[Typ](None, None, ser, serializer)(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](obj: Typ, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    apply(obj, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](node: Node, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    apply(node, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](ser: String, serializer: String)(implicit typ: Class[Typ]): IrreplaceableMetadataXml[Typ] =
    apply(ser, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)
}
