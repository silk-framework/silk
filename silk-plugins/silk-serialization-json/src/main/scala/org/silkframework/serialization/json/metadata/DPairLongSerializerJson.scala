package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.EntityMetadata
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers.numberValue
import org.silkframework.util.DPair
import play.api.libs.json.{JsNumber, JsObject, JsValue}

case class DPairLongSerializerJson(metaId: String) extends JsonMetadataSerializer[DPair[Long]] {
  /**
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  override def metadataId: String = metaId

  override def read(value: JsValue)(implicit readContext: ReadContext): DPair[Long] = {
    val source = numberValue(value, "Source").longValue()
    val target = numberValue(value, "Target").longValue()
    new DPair[Long](source, target)
  }

  override def write(value: DPair[Long])(implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsObject(Seq(
      "Source" -> JsNumber(value.source),
      "Target" -> JsNumber(value.target)
    ))
  }

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    *
    * @return
    */
  override def replaceableMetadata: Boolean = true
}
