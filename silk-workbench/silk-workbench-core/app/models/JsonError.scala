package models

import org.silkframework.runtime.validation.{ValidationError, ValidationException}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

object JsonError {

  def apply(message: String) = {
    Json.obj(
      "message" -> message
    )
  }

  def apply(exception: Throwable) = {
    Json.obj(
      "message" -> exception.getMessage
    )
  }

  def statusJson(message: String, errors: Seq[ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("errors", JsArray(errors.map(errorToJsExp))) ::
      ("warnings", JsArray(warnings.map(JsString(_)))) ::
      ("infos", JsArray(infos.map(JsString(_)))) :: Nil
    )
  }

}
