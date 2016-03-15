package models

import play.api.libs.json.{JsString, Json}

object JsonError {

  def apply(message: String) = {
    Json.obj(
      "message" -> message
    )
  }

  def apply(exception: Exception) = {
    Json.obj(
      "message" -> exception.getMessage
    )
  }

}
