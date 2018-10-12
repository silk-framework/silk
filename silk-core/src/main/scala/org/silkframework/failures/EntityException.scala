package org.silkframework.failures

import org.silkframework.entity.Path
import org.silkframework.util.Identifier

import scala.language.implicitConversions

/**
  * Wrapper Exception used for ease mapping of Throwable to [[FailureClass]]
  * NOTE: wrapping an exception in this object will not change the pertaining [[FailureClass]]
  * @param msg - override the forwarded message (if empty, the message of the actual exception is used)
  * @param ex - the initial exception (as received from the system)
  * @param taskId - the ID of the currently running task
  * @param property - an optional property path pointer (e.g. [[org.silkframework.entity.EntitySchema.typedPaths.headOption]]))
  */
class EntityException(msg: String, ex: Throwable, taskId: Identifier, property: Option[Path] = None) extends Exception(msg, ex){

  def this(msg: String, taskId: Identifier, property: Path) = this(msg, null, taskId, Some(property))

  private val exception = if(ex == null) this else ex

  private lazy val message = if(msg == null || msg.trim.isEmpty) exception.getMessage else msg

  /**
    * The pertaining [[FailureClass]] object
    */
  lazy val failureClass: FailureClass = FailureClass(FailureClass.getRootCause(exception), message, taskId, property)
}

object EntityException{
  implicit def toFailureClass(ee: EntityException): FailureClass = ee.failureClass
}
