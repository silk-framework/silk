package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariableName

/**
  * Thrown if a value for an unbound variable is missing.
  */
class UnboundVariablesException(val missingVars: Seq[TemplateVariableName], cause: Option[Exception] = None)
  extends TemplateEvaluationException(UnboundVariablesException.generateMessage(missingVars), cause) {

  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Unbound variables"


  /**
   * Include the unbound variables in the HTTP Problem details JSON.
   */
  override def additionalData: Map[String, Seq[String]] = {
    Map("unboundVariables" -> missingVars.map(_.scopedName))
  }
}

object UnboundVariablesException {

  private def generateMessage(missingVars: Seq[TemplateVariableName]): String = {
    missingVars match {
      case Seq(variable) =>
        s"'$variable' is not defined."
      case _ =>
        "The following variables are not defined: " + missingVars.mkString("'", "', '", "'")
    }

  }

}