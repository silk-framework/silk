package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariableName

/**
  * Thrown if a variable that is not sensitive references a sensitive variable of its own scope.
  * Reported instead of [[UnboundVariablesException]], because the referenced variable is defined, but withheld.
  */
class SensitiveVariableReferenceException(val sensitiveVars: Seq[TemplateVariableName])
  extends TemplateEvaluationException(SensitiveVariableReferenceException.message(sensitiveVars)) {

  override def errorTitle: String = "Reference to a sensitive variable"
}

object SensitiveVariableReferenceException {

  /** The error message for references to the given sensitive variables, also for reporting without an exception. */
  def message(sensitiveVars: Seq[TemplateVariableName]): String = {
    sensitiveVars match {
      case Seq(variable) =>
        s"'$variable' is sensitive and can only be referenced from a sensitive variable."
      case _ =>
        "The following variables are sensitive and can only be referenced from a sensitive variable: " + sensitiveVars.mkString("'", "', '", "'")
    }
  }

}
