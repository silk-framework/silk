package org.silkframework.runtime.validation

import org.silkframework.util.Identifier

/**
  * A generic validation issue.
  */
sealed trait ValidationIssue {

  /**
    * Human-readable explanation.
    */
  def message: String

  /**
    * The id of the element that is affected, such as a specific operator.
    */
  def id: Option[Identifier]

  /**
    * The type of element that is affected.
    */
  def elementType: Option[String]

  def issueType: String

  override def toString = id match {
    case Some(identifier) => s"$issueType in ${elementType.getOrElse("element")} with id '$identifier': $message"
    case None => message
  }

}

final case class ValidationError(message: String, id: Option[Identifier] = None, elementType: Option[String] = None) extends ValidationIssue {

  def issueType = "Error"

}

final case class ValidationWarning(message: String, id: Option[Identifier] = None, elementType: Option[String] = None) extends ValidationIssue {

  def issueType = "Warning"

}

final case class ValidationInfo(message: String, id: Option[Identifier] = None, elementType: Option[String] = None) extends ValidationIssue {

  def issueType = "Message"

}