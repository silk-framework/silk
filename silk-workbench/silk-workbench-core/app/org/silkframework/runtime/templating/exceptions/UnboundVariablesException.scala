package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.TemplateVariableName
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException.generateMessage
import org.silkframework.workbench.utils.JsonRequestException
import play.api.libs.json.{JsObject, Json}

/**
  * Thrown if a value for an unbound variable is missing.
  */
class UnboundVariablesException(val missingVars: Seq[TemplateVariableName], cause: Option[Exception] = None)
  extends TemplateEvaluationException(generateMessage(missingVars), cause) with JsonRequestException {

  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Unbound variables"

  /**
    * Json that will be included in addition to the HTTP Problem details JSON.
    * Note that using reserved HTTP Problem details fields (type, title, detail) would overwrite the generated ones.
    */
  override def additionalJson: JsObject = {
    Json.obj(
      "unboundVariables" -> missingVars.map(_.scopedName)
    )
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