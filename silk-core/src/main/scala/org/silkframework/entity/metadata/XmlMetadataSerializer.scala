package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{SerializationFormat, XmlFormat}

import scala.reflect._
import scala.xml.Node

abstract class XmlMetadataSerializer[T : ClassTag] extends XmlFormat[T] with MetadataSerializer {

  //add metadata serializer to registry
  //metadataId must be implemented as a def, see MetadataSerializer.metadataId
  //FIXME if in a future version of scala a trait such as 'OnCreation' is introduced, the forcing of implementation
  //methods can be dropped and the following registration can be placed inside such a onCreation method
  XmlMetadataSerializer.registerSerializationFormat(metadataId, this)
}

object XmlMetadataSerializer extends MetadataSerializerRegistry[Node] {

  /* register basic serializers for failures which are needed to add failures to entities */
  ExceptionSerializer()
  FailureClassSerializer()

  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  override val exceptionSerializer: SerializationFormat[GenericExecutionFailure, Node] with MetadataSerializer = ExceptionSerializer()
}
