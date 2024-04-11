package controllers.core.util

import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.serialization.json.JsonParseException
import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError, Reads}
import play.api.mvc.{AnyContent, Request}

import java.io.InputStream
import scala.util.control.NonFatal

/**
  * Some utility methods to work with (Play) Json.
  */
object JsonUtils {

  def validateJson[T](jsonStr: String)
                        (implicit rds: Reads[T]): T = {
    validateJsonFromValue(handleParseError(Json.parse(jsonStr)))
  }

  def validateJson[T](jsonIS: InputStream)
                        (implicit rds: Reads[T]): T = {
    validateJsonFromValue(handleParseError(Json.parse(jsonIS)))
  }

  private def handleParseError(jsValue: => JsValue): JsValue = {
    try {
      jsValue
    } catch {
      case NonFatal(ex) =>
        throw new JsonParseException("Could not parse Json", Some(ex))
    }
  }

  def validateJsonFromValue[T](jsValue: JsValue)
                              (implicit rds: Reads[T]): T = {
    val result = Json.fromJson[T](jsValue)
    result.fold(
      errors => {
        throw JsonParseException(errors)
      },
      obj => {
        obj
      }
    )
  }

  def validateJsonFromRequest[T](request: Request[AnyContent])
                                (implicit rds: Reads[T]): T = {
    request.body.asJson match {
      case Some(json) =>
        validateJsonFromValue[T](json)
      case None =>
        throw BadUserInputException("Expected JSON body.")
    }
  }
}
