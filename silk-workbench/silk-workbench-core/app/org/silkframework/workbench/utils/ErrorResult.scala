package org.silkframework.workbench.utils

import org.silkframework.runtime.validation.{ClientRequestException, ValidationIssue}
import org.silkframework.util.StringUtils._
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Status

/**
  * Formats errors as JSON.
  * Should be used whenever a REST Call returns an error.
  */
object ErrorResult {

  def apply(status: Int, title: String, detail: String): Result = {
    val json =
      Json.obj(
        "title" -> title,
        "detail" -> detail
      )

    Status(status)(json).as("application/problem+json")
  }

  def clientError(ex: ClientRequestException): Result = {
    ErrorResult(status = ex.errorCode, title = ex.errorText, ex.getMessage)
  }

  def serverError(status: Int, ex: Throwable): Result = {
    apply(status, ex.getClass.getSimpleName.undoCamelCase.replace("Exception", "Issue"), ex.getMessage)
  }

  def apply(message: String, issues: Seq[ValidationIssue]) = {
    Json.obj(
      "message" -> message,
      "issues" -> issues.map(validationMessage)
    )
  }

  private def validationMessage(msg: ValidationIssue) = {
    Json.obj(
      "type" -> msg.issueType,
      "message" -> msg.toString,
      "id" -> JsString(msg.id.map(_.toString).getOrElse(""))
    )
  }

}
