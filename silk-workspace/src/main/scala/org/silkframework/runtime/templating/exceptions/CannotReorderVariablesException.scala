package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.templating.exceptions.CannotReorderVariablesException.generateMessage
import org.silkframework.runtime.validation.RequestException
import org.silkframework.workbench.utils.JsonRequestException
import play.api.libs.json.{JsObject, Json}

import java.net.HttpURLConnection

/**
  * Thrown if the user tries to delete a variable that is used by other variables.
  */
case class CannotReorderVariablesException(dependencies: Map[String, Seq[String]], cause: Option[Throwable] = None)
  extends RequestException(generateMessage(dependencies), cause) with JsonRequestException {

  /**
    * A short error title, e.g, "Task not found".
    */
  override val errorTitle: String = "Cannot reorder variables. Templates can only access preceding variables (see details)."

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)

  /**
    * Json that will be included in addition to the HTTP Problem details JSON.
    */
  override def additionalJson: JsObject = {
    Json.obj(
      "dependencies" -> dependencies
    )
  }
}

object CannotReorderVariablesException {

  private def generateMessage(dependencies: Map[String, Seq[String]]): String = {
    val buffer = new StringBuilder()
    for((variable, dependentVariables) <- dependencies) {
      buffer ++= s"'$variable' depends on ${dependentVariables.mkString("'", "', '", "'")}. "
    }
    buffer.result()
  }

}