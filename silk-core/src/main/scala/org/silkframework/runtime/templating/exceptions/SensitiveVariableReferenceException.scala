package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariableName

/**
  * Thrown if a sensitive variable is referenced where it is withheld: from a variable of its own scope that is not
  * sensitive, or from a plugin parameter that does not accept sensitive variables.
  * Reported instead of [[UnboundVariablesException]], because the referenced variable is defined, but withheld.
  *
  * @param sensitiveVars    The referenced sensitive variables.
  * @param otherMissingVars Referenced variables that are not defined, reported along so that both problems show up at once.
  * @param inParameter      True if the reference is in the template of a plugin parameter, false if in the template of a variable.
  */
class SensitiveVariableReferenceException(val sensitiveVars: Seq[TemplateVariableName],
                                          val otherMissingVars: Seq[TemplateVariableName] = Seq.empty,
                                          inParameter: Boolean = false)
  extends TemplateEvaluationException(SensitiveVariableReferenceException.message(sensitiveVars, otherMissingVars, inParameter)) {

  override def errorTitle: String = "Reference to a sensitive variable"
}

object SensitiveVariableReferenceException {

  /** The error message for references to the given sensitive variables and, if any, to undefined ones, also for reporting without an exception. */
  def message(sensitiveVars: Seq[TemplateVariableName], otherMissingVars: Seq[TemplateVariableName] = Seq.empty, inParameter: Boolean = false): String = {
    val allowed = if (inParameter) "used in password parameters and in parameters that allow sensitive variables" else "referenced from a sensitive variable"
    val sensitiveMessage = sensitiveVars match {
      case Seq(variable) =>
        s"'$variable' is sensitive and can only be $allowed."
      case _ =>
        s"The following variables are sensitive and can only be $allowed: " + sensitiveVars.mkString("'", "', '", "'") + "."
    }
    if (otherMissingVars.isEmpty) sensitiveMessage else sensitiveMessage + " " + UnboundVariablesException.message(otherMissingVars)
  }

}
