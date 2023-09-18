package org.silkframework.entity.metadata

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.failures.{EntityException, FailureClass}
import org.silkframework.util.Identifier

/**
  * Holds metadata for each entity.
  *
  * @param failure The last failure that occurred on this entity (e.g., during transformation)
  * @param internalOrder Internal order id. Currently only used by Spark code.
  */
case class EntityMetadata(failure: Option[FailureClass] = None, internalOrder: Option[InternalOrder] = None) {

  def withFailure(failure: FailureClass): EntityMetadata = {
    copy(failure = Some(failure))
  }

  def withFailure(failure: Throwable, taskId: Identifier, property: Option[UntypedPath] = None): EntityMetadata = {
    withFailure(FailureClass(GenericExecutionFailure(failure), taskId, property))
  }
}

object EntityMetadata {

  def apply(ex: EntityException): EntityMetadata = {
    EntityMetadata(Some(ex.failureClass))
  }

}