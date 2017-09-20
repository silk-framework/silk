package org.silkframework.workbench.utils

import org.silkframework.runtime.validation.{RequestException, ValidationIssue}
import play.api.libs.json.{JsNull, JsString, JsValue, Json}
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
    * Generate a request error response.
    *
    * @param ex The exception that specifies the request error.
    */
  def requestError(ex: RequestException): Result = {
    generateResult(ex.httpErrorCode.getOrElse(500), fromException(ex))
  }

  /**
    * Generates a server error response.
    *
    * @param status The status code, e.g., 500.
    * @param ex The exception that has been thrown.
    */
  def serverError(status: Int, ex: Throwable): Result = {
    generateResult(status, fromException(ex))
  }

  /**
    * Generates an error response.
    */
  def apply(status: Int, title: String, detail: String): Result = {
    generateResult(status, format(title, detail))
  }

  private def fromException(ex: Throwable): JsValue = {
    val cause = Option(ex.getCause).map(fromException).getOrElse(JsNull)
    ex match {
      case requestEx: RequestException =>
        format(requestEx.errorTitle, requestEx.getMessage, cause)
      case _ =>
        val errorTitle = ex.getClass.getSimpleName.replace("Exception", "")
        val readableTitle = errorTitle.flatMap(c => if (c.isUpper) " " + c.toLower else c.toString).trim.capitalize
        format(readableTitle, ex.getMessage, cause)
    }
  }

  private def format(title: String, detail: String, cause: JsValue = JsNull): JsValue = {
    Json.obj(
      "title" -> title,
      "detail" -> detail,
      "cause" -> cause
    )
  }

  /**
    * Generates an error response with detailed issues.
    */
  def validation(status: Int, title: String, issues: Seq[ValidationIssue]): Result = {
    val json =
      Json.obj(
        "title" -> title,
        "detail" -> issues.headOption.map(_.message).getOrElse("Validation Error").toString,
        "issues" -> issues.map(validationMessage)
      )
    generateResult(status, json)
  }

  private def validationMessage(msg: ValidationIssue) = {
    Json.obj(
      "type" -> msg.issueType,
      "message" -> msg.toString,
      "id" -> JsString(msg.id.map(_.toString).getOrElse(""))
    )
  }

  private def generateResult(status: Int, value: JsValue): Result = {
    Status(status)(value).as("application/problem+json")
  }

}
