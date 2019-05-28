package controllers.core.util

import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json.{JsPath, Json, JsonValidationError, Reads}

/**
  * Some utility methods to work with (Play) Json.
  */
object JsonUtils {

  def validateJson[T, U](jsonStr: String)
                        (body: T => U)
                        (implicit rds: Reads[T]): U = {
    val jsValue = try {
      Json.parse(jsonStr)
    } catch {
      case e: Throwable =>
        throw new ValidationException("Could not parse Json", e)
    }
    val result = Json.fromJson[T](jsValue)
    result.fold(
      errors => {
        throw new ValidationException("Invalid JSON structure. Error details: " + errorsToString(errors))
      },
      obj => {
        body(obj)
      }
    )
  }

  def errorsToString(errors: Seq[(JsPath, Seq[JsonValidationError])]): String = {
    val errorStrings = errors map { case (path, validationErrors) =>
        "JSON Path \"" + path.toJsonString + "\" with error(s): " + validationErrors.map('"' + _.message + '"').mkString(", ")
    }
    errorStrings.mkString(", ")
  }
}
