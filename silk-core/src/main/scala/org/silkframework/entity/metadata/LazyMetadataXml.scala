package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{SerializationFormat, WriteContext, XmlFormat}

import scala.reflect.ClassTag
import scala.xml.{Group, Node}

case class LazyMetadataXml[Typ] private(
   obj: Option[Typ],
   serial: Option[Node],
   string: String,
   serializer: SerializationFormat[Typ, Node]
 )(implicit val typ: Class[Typ]) extends LazyMetadata[Typ, Node] {

  assert(obj.nonEmpty || serial.nonEmpty || string.nonEmpty, "LazyMetadata without any data object.")

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
    case None => Option(serializer.read(serialized))
  }

  /**
    * the raw, un-parsed metadata
    */
  override def serialized: Node = serial match{
    case Some(s) => s
    case None => string match{
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
}

object LazyMetadataXml{

  def apply[Typ](obj: Typ, serializer: SerializationFormat[Typ, Node])(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(Some(obj), None, "", serializer)(typ)

  def apply[Typ](node: Node, serializer: SerializationFormat[Typ, Node])(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(None, Some(node), "", serializer)(typ)

  def apply[Typ](ser: String, serializer: SerializationFormat[Typ, Node])(implicit typ: Class[Typ]): LazyMetadataXml[Typ] = apply(None, None, ser, serializer)(typ)
}
