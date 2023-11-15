package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariable
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException.generateMessage

/**
  * Thrown if a template variable could not be resolved, e.g., due to unbound variables.
  */
case class TemplateVariableEvaluationException(variable: TemplateVariable, ex: TemplateEvaluationException)
  extends Exception(s"Variable '${variable.name}': " + ex.getMessage, ex)

case class TemplateVariablesEvaluationException(issues: Seq[TemplateVariableEvaluationException]) extends Exception(generateMessage(issues))

object TemplateVariablesEvaluationException {

  private def generateMessage(issues: Seq[TemplateVariableEvaluationException]): String = {
    if(issues.size == 1) {
      issues.head.getMessage
    } else {
      "The following issues have been found: " + issues.map(_.getMessage).mkString(", ")
    }
  }

}
