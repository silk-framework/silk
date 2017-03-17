package controllers.core.util

import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc.{Controller, Request, Result}

/**
  * Utility methods useful in Controllers.
  */
trait ControllerUtilsTrait {
  this: Controller =>

  def validateJson[T](body: T => Result)
                     (implicit request: Request[JsValue],
                      rds: Reads[T]): Result = {
    val parsedObject = request.body.validate[T]
    parsedObject.fold(
      errors => {
        BadRequest(Json.obj("status" -> "JSON parse error", "message" -> JsError.toJson(errors)))
      },
      obj => {
        body(obj)
      }
    )
  }
}
