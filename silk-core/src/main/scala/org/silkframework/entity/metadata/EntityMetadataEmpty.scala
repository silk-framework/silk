package org.silkframework.entity.metadata
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}

import scala.collection.mutable
import scala.reflect.ClassTag


class EntityMetadataEmpty[Serialization] private (override val serTag: Class[Serialization]) extends EntityMetadata[Serialization]{

  type EMPTY >: Any <: Any
  private implicit val serClassTag = ClassTag(serTag)
  override val serializer: SerializationFormat[EntityMetadata[Serialization], Serialization] = new SerializationFormat[EntityMetadata[Serialization], Serialization]()(ClassTag(classOf[EntityMetadata[Serialization]]), ClassTag(serTag)) {
    /**
      * The MIME types that can be formatted.
      */
    override def mimeTypes: Set[String] = Set("")

    override def read(value: Serialization)(implicit readContext: ReadContext): EntityMetadata[Serialization] = EntityMetadataEmpty[Serialization](serTag)

    override def write(value: EntityMetadata[Serialization])(implicit writeContext: WriteContext[Serialization]): Serialization = null.asInstanceOf[Serialization]

    /**
      * Formats a value as string.
      */
    override def toString(value: EntityMetadata[Serialization], mimeType: String)(implicit writeContext: WriteContext[Serialization]): String = ""

    /**
      * Formats an iterable of values as string. The optional container name is used for formats where array like values
      * must be named, e.g. XML. This needs to be a valid serialization in the respective data model.
      */
    override def toString(value: Iterable[EntityMetadata[Serialization]], mimeType: String, containerName: Option[String])(implicit writeContext: WriteContext[Serialization]): String = ""

    /**
      * Reads a value from a string.
      */
    override def fromString(value: String, mimeType: String)(implicit readContext: ReadContext): EntityMetadata[Serialization] = EntityMetadataEmpty[Serialization](serTag)

    /**
      * Read Serialization format from string
      */
    override def parse(value: String, mimeType: String): Serialization = null.asInstanceOf[Serialization]
  }
  override val metadata: Map[String, LazyMetadata[_, Serialization]] = Map()

  override def addReplaceMetadata(key: String, lm: LazyMetadata[_, Serialization]): EntityMetadata[Serialization] = this
}

object EntityMetadataEmpty{

  private val emptyEntityMetadata = new mutable.HashMap[Class[_], EntityMetadataEmpty[_]]()

  def apply[Ser](serTag: Class[Ser]): EntityMetadataEmpty[Ser] = emptyEntityMetadata.get(serTag) match{
    case Some(s) => s.asInstanceOf[EntityMetadataEmpty[Ser]]
    case None =>
      val res = new EntityMetadataEmpty[Ser](serTag)
      emptyEntityMetadata.put(serTag, res)
      res
  }
}