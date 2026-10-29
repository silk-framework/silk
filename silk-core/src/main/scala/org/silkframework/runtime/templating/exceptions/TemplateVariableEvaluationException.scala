package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariable
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException.generateMessage
import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
  * Thrown if a template variable could not be resolved, e.g., due to unbound variables.
  */
case class TemplateVariableEvaluationException(variable: TemplateVariable, ex: TemplateEvaluationException)
  extends Exception(s"Variable '${variable.name}': " + ex.getMessage, ex)

/**
  * Aggregates the per-variable evaluation failures of resolving a set of template variables.
  * Like the single-variable [[TemplateEvaluationException]], this is a user-correctable input error.
  */
case class TemplateVariablesEvaluationException(issues: Seq[TemplateVariableEvaluationException])
  extends RequestException(generateMessage(issues), issues.headOption) {

  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Template evaluation error"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}

object TemplateVariablesEvaluationException {

  private def generateMessage(issues: Seq[TemplateVariableEvaluationException]): String = {
    if(issues.size == 1) {
      issues.head.getMessage
    } else {
      "The following issues have been found: " + issues.map(_.getMessage).mkString(", ")
    }
  }

}
