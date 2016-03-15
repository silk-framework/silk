package org.silkframework.runtime.validation

import org.silkframework.util.Identifier

case class ValidationError(message: String, id: Option[Identifier] = None, elementType: Option[String] = None) {
  override def toString = id match {
    case Some(identifier) => "Validation error in " + elementType.getOrElse("element") + " with id '" + identifier + "': " + message
    case None => message
  }
}