package org.silkframework.workbench.utils

import org.silkframework.runtime.validation.{ClientRequestException, ValidationIssue}
import org.silkframework.util.StringUtils._
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Status

/**
  * Generates error responses.
  * The response format is based on RFC 7807 "Problem Details for HTTP APIs".
  * Should be used whenever a REST Call returns an error.
  *
  * See: [[https://tools.ietf.org/html/rfc7807]].
  */
object ErrorResult {

  /**
    * Generate a client error response.
    *
    * @param ex The exception that specifies the client error.
    */
  def clientError(ex: ClientRequestException): Result = {
    apply(status = ex.errorCode, title = ex.errorText, ex.getMessage)
  }

  /**
    * Generates a server error response.
    *
    * @param status The status code, e.g., 500.
    * @param ex The exception that has been thrown.
    */
  def serverError(status: Int, ex: Throwable): Result = {
    apply(status, ex.getClass.getSimpleName.undoCamelCase.replace("Exception", "Issue"), ex.getMessage)
  }

  /**
    * Generates an error response.
    */
  def apply(status: Int, title: String, detail: String): Result = {
    val json =
      Json.obj(
        "title" -> title,
        "detail" -> detail
      )

    Status(status)(json).as("application/problem+json")
  }

  /**
    * Generates an error response with detailed issues.
    * Not using Problem Details for HTTP APIs yet.
    */
  def apply(message: String, issues: Seq[ValidationIssue]): JsValue = {
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
