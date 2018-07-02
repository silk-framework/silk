package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{SerializationFormat, XmlFormat}

import scala.reflect.ClassTag
import scala.xml.Node

abstract class XmlMetadataSerializer[T : ClassTag] extends XmlFormat[T]{

  /**
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  val metadataId: String

  //add metadata serializer to registry
  XmlMetadataSerializer.registerSerializationFormat(metadataId, this)
}

object XmlMetadataSerializer extends MetadataRegistry[Node] {
  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  override val exceptionSerializer: SerializationFormat[Throwable, Node] = ExceptionSerializer()
}