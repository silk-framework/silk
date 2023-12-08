package org.silkframework.workbench.utils

import org.silkframework.runtime.validation.{RequestException, ValidationIssue}
import org.silkframework.util.StringUtils.toStringUtils
import play.api.libs.json.{Format, JsNull, JsObject, JsString, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Status

import scala.collection.immutable.ArraySeq

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
  def apply(ex: RequestException): Result = {
    val status = ex.httpErrorCode.getOrElse(500)
    generateResult(status, fromException(ex, (status / 100) == 5))
  }

  /**
    * Generates a server error response.
    *
    * @param status The status code, e.g., 500.
    * @param ex The exception that has been thrown.
    */
  def serverError(status: Int, ex: Throwable): Result = {
    val addStacktrace = (status / 100) == 5
    generateResult(status, fromException(ex, addStacktrace))
  }

  /**
    * Generates an error response.
    */
  def apply(status: Int, title: String, detail: String): Result = {
    generateResult(status, format(title, detail))
  }

  private def fromException(ex: Throwable, addStacktrace: Boolean): JsValue = {
    val stacktrace = if(addStacktrace) Json.obj("stacktrace" -> ErrorResult.Stacktrace.fromException(ex)) else Json.obj()
    val cause = Option(ex.getCause).map(cause => fromException(cause, addStacktrace = false)).getOrElse(JsNull)
    ex match {
      case requestEx: RequestException with JsonRequestException =>
        format(requestEx.errorTitle, requestEx.getMessage, cause, requestEx.additionalJson ++ stacktrace)
      case requestEx: RequestException =>
        format(requestEx.errorTitle, requestEx.getMessage, cause, stacktrace)
      case _ =>
        val errorTitle = ex.getClass.getSimpleName.replace("Exception", "Error")
        val readableTitle = errorTitle.toSentenceCase
        format(readableTitle, ex.getMessage, cause, stacktrace)
    }
  }

  private def format(title: String, detail: String, cause: JsValue = JsNull, additionalJson: JsObject = JsObject.empty): JsValue = {
    Json.obj(
      "title" -> title,
      "detail" -> detail,
      "cause" -> cause
    ) ++ additionalJson
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

  /** Stacktrace object. */
  case class Stacktrace(exceptionClass: String, errorMessage: Option[String], lines: Seq[String], cause: Option[Stacktrace], suppressed: Seq[Stacktrace])

  object Stacktrace {
    implicit val stacktraceJsonFormat: Format[Stacktrace] = Json.format[Stacktrace]

    def fromException(exception: Throwable): Stacktrace = {
      val lines = ArraySeq.unsafeWrapArray(exception.getStackTrace.map(_.toString))
      val cause = Option(exception.getCause).map(fromException)
      val suppressed = ArraySeq.unsafeWrapArray(exception.getSuppressed).map(fromException)
      Stacktrace(exception.getClass.getName, Option(exception.getMessage), lines, cause, suppressed)
    }
  }
}