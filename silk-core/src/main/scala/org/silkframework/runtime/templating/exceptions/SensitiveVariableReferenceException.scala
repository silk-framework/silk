package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariableName

/**
  * Thrown if a variable that is not sensitive references a sensitive variable of its own scope.
  * Reported instead of [[UnboundVariablesException]], because the referenced variable is defined, but withheld.
  *
  * @param sensitiveVars    The referenced sensitive variables.
  * @param otherMissingVars Referenced variables that are not defined, reported along so that both problems show up at once.
  */
class SensitiveVariableReferenceException(val sensitiveVars: Seq[TemplateVariableName], val otherMissingVars: Seq[TemplateVariableName] = Seq.empty)
  extends TemplateEvaluationException(SensitiveVariableReferenceException.message(sensitiveVars, otherMissingVars)) {

  override def errorTitle: String = "Reference to a sensitive variable"
}

object SensitiveVariableReferenceException {

  /** The error message for references to the given sensitive variables and, if any, to undefined ones, also for reporting without an exception. */
  def message(sensitiveVars: Seq[TemplateVariableName], otherMissingVars: Seq[TemplateVariableName] = Seq.empty): String = {
    val sensitiveMessage = sensitiveVars match {
      case Seq(variable) =>
        s"'$variable' is sensitive and can only be referenced from a sensitive variable."
      case _ =>
        "The following variables are sensitive and can only be referenced from a sensitive variable: " + sensitiveVars.mkString("'", "', '", "'") + "."
    }
    if (otherMissingVars.isEmpty) sensitiveMessage else sensitiveMessage + " " + UnboundVariablesException.message(otherMissingVars)
  }

}
