package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext, XmlFormat}

import scala.reflect.ClassTag
import scala.xml.{Group, Node}

class LazyMetadataXml[Typ] private[metadata](
  private[metadata] val obj: Option[Typ],
  private[metadata] val serial: Option[Node],
  private[metadata] val str: String,
  override val serializer: SerializationFormat[Typ, Node] with MetadataSerializer
 )(implicit val typ: Class[Typ]) extends LazyMetadata[Typ, Node] {

  override implicit val serTag: ClassTag[Node] = ClassTag(classOf[Node])
  override implicit val typTag: ClassTag[Typ] = ClassTag(typ)
  private implicit val nodeWc = WriteContext[Node]()
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
  override def serialized: Node = serial match{
    case Some(s) => s
    case None => str match{
      case s: String if s.trim.nonEmpty => serializer.parse(s, defaultMimeType)
      case _ => obj match{
        case Some(x) => serializer.write(x)
        case None => Group(Seq())
      }
    }
  }

  /**
    * Providing the default mime type to be used with the serializer
    */
  override val defaultMimeType: String = XmlFormat.MIME_TYPE_TEXT

  /**
    * String representation of the serialized object
    */
  override val string: String = if(str.trim.nonEmpty){
    str
  }
  else{
    serialized.toString()
  }

  /**
    * indicates whether this is an empty LazyMetadata instance
    */
  override def isEmpty: Boolean = obj.isEmpty && serial.isEmpty && str.trim.isEmpty
}

object LazyMetadataXml{

  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] =
    new LazyMetadataXml(Some(obj), None, "", serializer)(typ)

  def apply[Typ](node: Node, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] =
    new LazyMetadataXml(None, Some(node), "", serializer)(typ)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, Node] with MetadataSerializer)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] =
    new LazyMetadataXml(None, None, ser, serializer)(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](obj: Typ, serializer: String)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(obj, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](node: Node, serializer: String)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(node, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)

  @throws[IllegalArgumentException]
  def apply[Typ](ser: String, serializer: String)(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(ser, XmlMetadataSerializer.getSerializer[Typ](serializer))(typ)
}
