package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.XmlFormat

import scala.reflect.ClassTag
import scala.xml.Node

abstract class XmlMetadata[T : ClassTag] extends XmlFormat[T]{

  /**
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  val metadataId: String

  //add metadata serializer to registry
  XmlMetadata.registerSerializationFormat(metadataId, this)
}

object XmlMetadata extends MetadataRegistry[Node]{

}
