package org.silkframework.serialization.json.metadata

import org.silkframework.entity.Path
import org.silkframework.entity.metadata.{ExecutionFailure, FailureClassSerializer}
import org.silkframework.failures.{AccumulatedFailureClass, FailureClass}
import org.silkframework.failures.FailureClass.{TASK_ID_TAG, _}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json
import play.api.libs.json.{JsObject, JsValue}
import org.silkframework.serialization.json.JsonHelpers._

case class FailureClassSerializerJson() extends JsonMetadataSerializer[FailureClass] {

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
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
    val property = stringValueOption(value, PROPERTY_TAG).map(Path(_))
    val accumulated = booleanValue(value, ACUUMULATED_TAG)
    val fc = FailureClass(ExecutionFailure.asThrowable(rootCause), originalMessage, taskId, property)
    if(accumulated) {
      new AccumulatedFailureClass(fc)
    }
    else {
      fc
    }
  }

  override def write(value: FailureClass)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsObject(Seq(
      ROOT_CAUSE_TAG -> ExceptionSerializerJson().write(ExecutionFailure.fromThrowable(value.rootCause)),
      MESSAGE_TAG -> json.JsString(value.originalMessage),
      TASK_ID_TAG -> json.JsString(value.taskId),
      ACUUMULATED_TAG -> json.JsBoolean(value.accumulated())
    ) ++
      value.property.map(p => PROPERTY_TAG -> json.JsString(p.normalizedSerialization))
    )
  }
}
