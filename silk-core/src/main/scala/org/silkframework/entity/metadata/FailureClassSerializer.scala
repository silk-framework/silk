package org.silkframework.entity.metadata

import org.silkframework.entity.Path
import org.silkframework.failures.{AccumulatedFailureClass, FailureClass}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.failures.FailureClass._

import scala.xml.Node

case class FailureClassSerializer() extends XmlMetadataSerializer[FailureClass] {

  override def read(node: Node)(implicit readContext: ReadContext): FailureClass = {
    val taskId = (node \ TASK_ID_TAG).text.trim
    val message = (node \ MESSAGE_TAG).text.trim
    val rootCause = ExceptionSerializer().readException((node \ ROOT_CAUSE_TAG).headOption.flatMap(_.child.headOption).orNull)
    val property = (node \ PROPERTY_TAG).headOption.map(p => Path(p.text))
    val accumulated = (node \ ACUUMULATED_TAG).text.trim.toBoolean
    val fc = FailureClass(ExecutionFailure.asThrowable(rootCause), message, taskId, property)

    if(accumulated) {
      new AccumulatedFailureClass(fc)
    }
    else {
      fc
    }

  }

  override def write(fc: FailureClass)(implicit writeContext: WriteContext[Node]): Node = {
    <FailureClass>
      <RootCause>{ExceptionSerializer().write(ExecutionFailure.fromThrowable(fc.rootCause))}</RootCause>
      <Message>{fc.originalMessage}</Message>
      <TaskId>{fc.taskId}</TaskId>
      <Property>{fc.property}</Property>
      <Accumulated>{fc.accumulated()}</Accumulated>
    </FailureClass>
  }

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  override def metadataId: String = FailureClassSerializer.METADATA_ID

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    */
  override def replaceableMetadata: Boolean = false
}

object FailureClassSerializer{
  val METADATA_ID: String = "failure_class"
}
