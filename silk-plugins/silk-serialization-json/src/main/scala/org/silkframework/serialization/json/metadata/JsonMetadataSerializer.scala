package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{EntityMetadata, MetadataRegistry}
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import play.api.libs.json.JsValue

import scala.reflect.ClassTag

abstract class JsonMetadataSerializer[T : ClassTag] extends JsonFormat[T]{

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    */
  val metadataId: String

  //add metadata serializer to registry
  JsonMetadataSerializer.registerSerializationFormat(metadataId, this)
}

object JsonMetadataSerializer extends MetadataRegistry[JsValue] {
  /**
    * Each serialization format needs a dedicated Exception serializer
    */
  override val exceptionSerializer: SerializationFormat[Throwable, JsValue] = new JsonMetadataSerializer[Throwable] {
    /**
      * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
      */
    override val metadataId: String = EntityMetadata.FAILURE_KEY

    override def read(value: JsValue)(implicit readContext: ReadContext): Throwable = ??? //TODO

    override def write(value: Throwable)(implicit writeContext: WriteContext[JsValue]): JsValue = ???
  }
}