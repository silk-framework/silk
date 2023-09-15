package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.FailureClassSerializer
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.failures.FailureClass._
import org.silkframework.failures.{AccumulatedFailureClass, FailureClass}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import play.api.libs.json
import play.api.libs.json.{JsObject, JsValue}

case class FailureClassSerializerJson() extends JsonMetadataSerializer[FailureClass] {

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadataLegacy]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  override def metadataId: String = FailureClassSerializer.METADATA_ID

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    *
    * @return
    */
  override def replaceableMetadata: Boolean = false

  override def read(value: JsValue)(implicit readContext: ReadContext): FailureClass = {
    val rootCause = ExceptionSerializerJson().read((value \ ROOT_CAUSE_TAG).toOption.get)
    val originalMessage = stringValue(value, MESSAGE_TAG)
    val taskId = stringValue(value, TASK_ID_TAG)
    val property = stringValueOption(value, PROPERTY_TAG).map(UntypedPath(_))
    val accumulated = booleanValue(value, ACCUMULATED_TAG)
    val fc = FailureClass(rootCause, originalMessage, taskId, property)
    if(accumulated) {
      new AccumulatedFailureClass(fc)
    }
    else {
      fc
    }
  }

  override def write(value: FailureClass)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsObject(Seq(
      ROOT_CAUSE_TAG -> ExceptionSerializerJson().write(value.rootCause),
      MESSAGE_TAG -> json.JsString(value.originalMessage),
      TASK_ID_TAG -> json.JsString(value.taskId),
      ACCUMULATED_TAG -> json.JsBoolean(value.accumulated())
    ) ++
      value.property.map(p => PROPERTY_TAG -> json.JsString(p.normalizedSerialization))
    )
  }
}
