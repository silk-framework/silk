package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{SerializationFormat, XmlFormat}
import org.silkframework.util.ScalaReflectUtils

import scala.reflect._
import scala.xml.Node

abstract class XmlMetadataSerializer[T : ClassTag] extends XmlFormat[T] with MetadataSerializer {

  //we have to make sure that metadataId was not implemented as a val
  if(! ScalaReflectUtils.implementedAsDef("metadataId", this.getClass))
    throw new NotImplementedError("Method metadataId in " + this.getClass.getName + " was implemented as a val. Make sure to implement this method as a def!")

  //add metadata serializer to registry
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
  override val exceptionSerializer: SerializationFormat[ExecutionFailure, Node] with MetadataSerializer = ExceptionSerializer()
}
