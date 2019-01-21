package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{GenericExecutionFailure, MetadataSerializer, MetadataSerializerRegistry}
import org.silkframework.runtime.serialization.SerializationFormat
import org.silkframework.serialization.json.JsonFormat
import org.silkframework.util.ScalaReflectUtils
import play.api.libs.json.JsValue

import scala.reflect.ClassTag

abstract class JsonMetadataSerializer[T : ClassTag] extends JsonFormat[T] with MetadataSerializer {

  //we have to make sure that metadataId was not implemented as a val
  if(! ScalaReflectUtils.implementedAsDef("metadataId", this.getClass))
    throw new NotImplementedError("Method metadataId in " + this.getClass.getName + " was implemented as a val. Make sure to implement this method as a def!")

  //add metadata serializer to registry
  //FIXME if in a future version of scala a trait such as 'OnCreation' is introduced, the forcing of implementation
  //methods can be dropped and the following registration can be placed inside such a onCreation method
  JsonMetadataSerializer.registerSerializationFormat(metadataId, this)
}

object JsonMetadataSerializer extends MetadataSerializerRegistry[JsValue] {

  /* register basic serializers for failures which are needed to add failures to entities */
  ExceptionSerializerJson()
  FailureClassSerializerJson()

  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  override val exceptionSerializer: SerializationFormat[GenericExecutionFailure, JsValue] with MetadataSerializer = ExceptionSerializerJson()
}
