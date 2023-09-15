package org.silkframework.serialization.json.metadata
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.{JsString, JsValue}

case class StringSerializerJson(metaId: String, replaceableMetadata: Boolean) extends JsonMetadataSerializer[String] {
  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadataLegacy]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  override def metadataId: String = metaId

  override def read(value: JsValue)(implicit readContext: ReadContext): String = value.toString().stripPrefix("\"").stripSuffix("\"")

  override def write(value: String)(implicit writeContext: WriteContext[JsValue]): JsValue = JsString(value)
}
