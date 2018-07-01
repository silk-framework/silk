package org.silkframework.serialization.json

import org.silkframework.entity.metadata.LazyMetadata.Schema
import org.silkframework.entity.metadata.LazyMetadata
import org.silkframework.runtime.serialization.SerializationFormat
import play.api.libs.json.JsValue

import scala.reflect.ClassTag

case class LazyMetadataJson[T](
  key: String,
  override val serialized: JsValue,
  override val serializer: SerializationFormat[T, JsValue]
)(implicit ct: Class[T]) extends LazyMetadata[T, JsValue] {

  override implicit val typTag: ClassTag[T] = ClassTag(ct)
  override implicit val serTag: ClassTag[JsValue] = ClassTag(classOf[JsValue])

  /**
    * the schema object defining the parsed metadata object
    */
  override val schema: Schema = None
  //TODO
  /**
    * String representation of the serialized object
    */
  override val string: String = ""
  /**
    * the final metadata object lazily computed
    * NOTE: should be implemented as lazy val!
    */
  override val metadata: Option[T] = None
}
