package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.validation.RequestException
import org.silkframework.workbench.utils.JsonRequestException
import play.api.libs.json.{JsObject, Json}

import java.net.HttpURLConnection

/**
  * Thrown if the user tries to delete a variable that is used by other variables.
  */
case class CannotDeleteUsedVariableException(variable: String, dependentVariables: Seq[String], cause: Option[Throwable] = None)
  extends RequestException(s"The variable '$variable' cannot be deleted because it's used in the following other variables: " +
    dependentVariables.mkString("'", "', '", "'"), cause) with JsonRequestException {

  /**
    * A short error title, e.g, "Task not found".
    */
  override val errorTitle: String = "Cannot delete used variable"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)

  /**
    * Json that will be included in addition to the HTTP Problem details JSON.
    */
  override def additionalJson: JsObject = {
    Json.obj(
      "variable" -> variable,
      "dependentVariables" -> dependentVariables
    )
  }
}