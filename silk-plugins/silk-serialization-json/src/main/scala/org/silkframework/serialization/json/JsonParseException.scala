package org.silkframework.serialization.json

import java.net.HttpURLConnection
import org.silkframework.runtime.validation.RequestException
import play.api.libs.json.{JsPath, JsonValidationError}

/**
  * Exception thrown while parsing JSON.
  */
case class JsonParseException(msg: String, cause: Option[Throwable] = None) extends RequestException(msg, cause) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Could not parse JSON"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}

object JsonParseException {

  /**
   * Creates an exception from Play JSON validation errors.
   */
  def apply(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]): JsonParseException = {
    val errorStrings = errors map { case (path, validationErrors) =>
      "JSON Path '" + path.toJsonString + "' with error(s): " + validationErrors.map("'" + _.message + "'").mkString(", ")
    }
    JsonParseException(errorStrings.mkString(", "))
  }

}