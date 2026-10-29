package org.silkframework.runtime.templating.exceptions

/**
  * Thrown if a template is malformed, e.g. contains a syntax error in an expression.
  * In contrast to other evaluation errors, this error does not depend on the provided variable values.
  */
class TemplateSyntaxException(msg: String, cause: Option[Exception] = None) extends TemplateEvaluationException(msg, cause) {

  override def errorTitle: String = "Template syntax error"
}
