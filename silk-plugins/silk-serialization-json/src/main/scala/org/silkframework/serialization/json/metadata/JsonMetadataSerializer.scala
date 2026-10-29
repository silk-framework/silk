package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{GenericExecutionFailure, MetadataSerializer, MetadataSerializerRegistry}
import org.silkframework.runtime.serialization.SerializationFormat
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.JsValue

import scala.reflect.ClassTag

abstract class JsonMetadataSerializer[T : ClassTag] extends JsonFormat[T] with MetadataSerializer {

  //add metadata serializer to registry
  //metadataId must be implemented as a def, see MetadataSerializer.metadataId
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
