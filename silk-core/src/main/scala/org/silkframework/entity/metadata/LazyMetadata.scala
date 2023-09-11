package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}

import scala.reflect.ClassTag

/**
  * Defines a metadata object with a raw String, Schema and a parser function which is executed at most once
  * Serialization follows this pattern metadata[Typ] -> serialized[Ser] -> string[String]
  * @tparam Typ - The target type of the metadata object
  * @tparam Ser - The type of the serialization Format (e.g. [[scala.xml.Node]] for Xml serialization)
  */
trait LazyMetadata[Typ, Ser] extends Serializable {

  implicit val typTag: ClassTag[Typ]
  implicit val serTag: ClassTag[Ser]

  /**
    * String representation of the serialized object
    */
  val string: String

  /**
    * the raw, un-parsed metadata
    */
  def serialized: Ser

  /**
    * the schema object defining the parsed metadata object
    */
  val schema: LazyMetadata.Schema

  /**
    * the parser function to parse the target type
    * NOTE: should be implemented as def (is transient)
    * usually pass the metadata category key and get the serializer from the serialization type registry
    */
  @transient def serializer: SerializationFormat[Typ, Ser] with MetadataSerializer

  /**
    * the final metadata object lazily computed
    * NOTE: should be implemented as lazy val!
    */
  def metadata: Option[Typ]

  /**
    * Providing the default mime type to be used with the serializer
    */
  val defaultMimeType: String

  /**
    * Indicates whether this metadata object might be overwritten by subsequent object with the same metadataId
    */
  val isReplaceable: Boolean = ! classOf[IrreplaceableMetadata].isAssignableFrom(this.getClass)

  /**
    * indicates whether this is an empty LazyMetadata instance
    */
  def isEmpty: Boolean

  override def toString: String = string
}

/**
  * Objects claiming this trait cannot be replaced or deleted from an EntityMetadata map
  */
trait IrreplaceableMetadata

object LazyMetadata extends Serializable {

  type Schema = Any

  def nullSerializer[Typ, Ser](implicit ct: ClassTag[Typ], st: ClassTag[Ser]): SerializationFormat[Typ, Ser] with MetadataSerializer = new SerializationFormat[Typ, Ser] with MetadataSerializer {
    override def mimeTypes: Set[String] = Set()
    override def read(value: Ser)(implicit readContext: ReadContext): Typ = null.asInstanceOf[Typ]
    override def write(value: Typ)(implicit writeContext: WriteContext[Ser]): Ser = null.asInstanceOf[Ser]
    override def toString(value: Typ, mimeType: String)(implicit writeContext: WriteContext[Ser]): String = ""
    override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): Typ = null.asInstanceOf[Typ]
    override def toString(value: Iterable[Typ], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[Ser]): String = ""
    override def parse(value: String, mimeType: String): Ser = null.asInstanceOf[Ser]
    override def metadataId: String = "NULL_SERIALIZER"
    override def replaceableMetadata: Boolean = true
  }

  def singleValueSerializer[Typ](theVal: Typ)(implicit ct: ClassTag[Typ]): SerializationFormat[Typ, String] with MetadataSerializer = new SerializationFormat[Typ, String] with MetadataSerializer {
    override def mimeTypes: Set[String] = Set()
    override def read(value: String)(implicit readContext: ReadContext): Typ = theVal
    override def write(value: Typ)(implicit writeContext: WriteContext[String]): String = theVal.toString
    override def toString(value: Typ, mimeType: String)(implicit writeContext: WriteContext[String]): String = theVal.toString
    override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): Typ = theVal
    override def toString(value: Iterable[Typ], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[String]): String = theVal.toString
    override def parse(value: String, mimeType: String): String = theVal.toString
    override def metadataId: String = "SINGLE_VALUE_SERIALIZER"
    override def replaceableMetadata: Boolean = true
  }

  def empty[Typ, Ser <: Any](implicit tt: Class[Typ], st: Class[Ser]): LazyMetadata[Typ, Ser] = new LazyMetadata[Typ, Ser] {

    override implicit val typTag: ClassTag[Typ] = ClassTag(tt)
    override implicit val serTag: ClassTag[Ser] = ClassTag(st)

    /**
      * the raw, un-parsed metadata
      */
    override val serialized: Ser = null.asInstanceOf[Ser]
    /**
      * the schema object defining the parsed metadata object
      */
    override val schema: Schema = None
    /**
      * the parser function to parse the target type
      */
    override val serializer: SerializationFormat[Typ, Ser] with MetadataSerializer = nullSerializer[Typ, Ser]
    /**
      * String representation of the serialized object
      */
    override val string: String = ""
    /**
      * the final metadata object lazily computed
      * NOTE: should be implemented as lazy val!
      */
    override val metadata: Option[Typ] = None
    /**
      * Providing the default mime type to be used with the serializer
      */
    override val defaultMimeType: String = ""

    /**
      * indicates whether this is an empty LazyMetadata instance
      */
    override def isEmpty: Boolean = true
  }

  /**
    * This will create a generic instance of LazyMetadata, acting similar to an Option[Typ]
    */
  def apply[Typ](value: Typ)(implicit typ: Class[Typ]): LazyMetadata[Typ, String] = new LazyMetadata[Typ, String] {

    override implicit val typTag: ClassTag[Typ] = ClassTag(typ)
    override implicit val serTag: ClassTag[String] = ClassTag(classOf[String])

    /**
      * the schema object defining the parsed metadata object
      */
    override val schema: Schema = None
    /**
      * the parser function to parse the target type
      */
    override val serializer: SerializationFormat[Typ, String] with MetadataSerializer = singleValueSerializer(value)
    /**
      * the raw, un-parsed metadata
      */
    override def serialized: String = value.toString

    /**
      * String representation of the serialized object
      */
    override val string: String = value.toString
    /**
      * the final metadata object lazily computed
      * NOTE: should be implemented as lazy val!
      */
    override val metadata: Option[Typ] = Some(value)
    /**
      * Providing the default mime type to be used with the serializer
      */
    override val defaultMimeType: String = ""

    /**
      * indicates whether this is an empty LazyMetadata instance
      */
    override def isEmpty: Boolean = metadata.isEmpty
  }
}
