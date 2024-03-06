package org.silkframework.workbench.utils

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.runtime.validation.{RequestException, ValidationIssue}
import org.silkframework.util.StringUtils.toStringUtils
import play.api.libs.json.{Format, JsNull, JsObject, JsString, JsValue, Json, OWrites}
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
  def apply(ex: RequestException, includeStacktrace: Option[Boolean] = None): Result = {
    val status = ex.httpErrorCode.getOrElse(500)
    val addStacktrace = includeStacktrace.getOrElse((status / 100) == 5)
    generateResult(status, fromException(ex, addStacktrace), additionalJson(ex))
  }

  /**
    * Generates a server error response.
    *
    * @param status The status code, e.g., 500.
    * @param ex The exception that has been thrown.
    */
  def serverError(status: Int, ex: Throwable): Result = {
    val addStacktrace = (status / 100) == 5
    generateResult(status, fromException(ex, addStacktrace), additionalJson(ex))
  }

  /**
    * Generates an error response.
    */
  def apply(status: Int, title: String, detail: String): Result = {
    generateResult(status, ErrorResultFormat(title, detail))
  }

  private def fromException(ex: Throwable, addStacktrace: Boolean): ErrorResultFormat = {
    val stacktrace = if(addStacktrace) Some(ErrorResult.Stacktrace.fromException(ex)) else None
    val cause = Option(ex.getCause).map(cause => fromException(cause, addStacktrace = false))
    ex match {
      case requestEx: RequestException =>
        ErrorResultFormat(requestEx.errorTitle, requestEx.getMessage, cause, stacktrace)
      case _ =>
        val errorTitle = ex.getClass.getSimpleName.replace("Exception", "Error")
        val readableTitle = errorTitle.toSentenceCase
        ErrorResultFormat(readableTitle, ex.getMessage, cause, stacktrace)
    }
  }

  private def additionalJson(ex: Throwable): JsObject = {
    ex match {
      case requestEx: RequestException with JsonRequestException =>
        requestEx.additionalJson
      case _ =>
        Json.obj()
    }
  }

  /**
    * Generates an error response with detailed issues.
    */
  def validation(status: Int, title: String, issues: Seq[ValidationIssue]): Result = {
    val error = ErrorResultFormat(
      title = title,
      detail = issues.headOption.map(_.message).getOrElse("Validation Error"),
      issues = Some(issues.map(ValidationIssueFormat(_)))
    )
    generateResult(status, error)
  }

  private def generateResult(status: Int, value: ErrorResultFormat, additionalJson: JsObject = Json.obj()): Result = {
    val json = Json.toJsObject(value) ++ additionalJson
    Status(status)(json).as("application/problem+json")
  }

  @Schema(description = "HTTP Problem Details format with some extensions. See: https://datatracker.ietf.org/doc/html/rfc7807")
  case class ErrorResultFormat(@Schema(
                                 description = "A short description of the error type. Should be the same for all instances of an error type.",
                                 example = "Task not found"
                               )
                               title: String,
                               @Schema(
                                 description = "Detailed error description.",
                                 example = "Project 'my project' does not contain a task 'my task'."
                               )
                               detail: String,
                               @Schema(
                                 description = "Optional cause of this error. Might include more detailed error descriptions.",
                                 implementation = classOf[ErrorResultFormat],
                                 requiredMode = RequiredMode.NOT_REQUIRED,
                                 nullable = true
                               )
                               cause: Option[ErrorResultFormat] = None,
                               @Schema(
                                 description = "Internal stacktrace.",
                                 implementation = classOf[Stacktrace],
                                 requiredMode = RequiredMode.NOT_REQUIRED,
                                 nullable = true
                               )
                               stacktrace: Option[Stacktrace] = None,
                               @ArraySchema(
                                 schema = new Schema(
                                   description = "Detailed list of issues. Provided if rules are accessed/edited.",
                                   implementation = classOf[ValidationIssueFormat])
                               )
                               issues: Option[Seq[ValidationIssueFormat]] = None)

  object ErrorResultFormat {
    implicit val errorResultFormat: OWrites[ErrorResultFormat] = Json.writes[ErrorResultFormat]
  }

  @Schema(description = "Issue in a rule")
  case class ValidationIssueFormat(@Schema(
                                     description = "Severity of this issue",
                                     example = "Error",
                                     allowableValues = Array("Error","Warning","Info")
                                   )
                                   `type`: String,
                                   @Schema(
                                     description = "Description of this issue",
                                     example = "Parameter 'numLines' cannot be negative."
                                   )
                                   message: String,
                                   @Schema(
                                     description = "The id of the element that is affected, such as a specific operator",
                                     example = "SkipLines"
                                   )
                                   id: String)

  object ValidationIssueFormat {

    implicit val jsonFormat: OWrites[ValidationIssueFormat] = Json.writes[ValidationIssueFormat]

    def apply(msg: ValidationIssue): ValidationIssueFormat = {
      ValidationIssueFormat(
        `type` = msg.issueType,
        message = msg.toString,
        id = msg.id.map(_.toString).getOrElse("")
      )
    }
  }

  @Schema(description = "Stacktrace of the exception that has been thrown internally.")
  case class Stacktrace(@Schema(
                          description = "The full name of the class including its package.",
                          example = "org.silkframework.workspace.exceptions.TaskNotFoundException"
                        )
                        exceptionClass: String,
                        @Schema(
                          description = "The error message",
                          example = "Project 'my project' does not contain a task 'my task'.",
                          requiredMode = RequiredMode.NOT_REQUIRED,
                          nullable = true
                        )
                        errorMessage: Option[String],
                        @ArraySchema(
                          schema = new Schema(
                            description = "Array of stack trace elements, each representing one stack frame",
                            example = "org.silkframework.workspace.Workspace.project(Workspace.scala:140)",
                            implementation = classOf[String]
                          )
                        )
                        lines: Seq[String],
                        @Schema(
                          description = "Cause of this exception",
                          implementation = classOf[Stacktrace],
                          requiredMode = RequiredMode.NOT_REQUIRED,
                          nullable = true
                        )
                        cause: Option[Stacktrace],
                        @ArraySchema(
                          schema = new Schema(
                            description = "Suppressed exceptions",
                            implementation = classOf[Stacktrace]
                          )
                        )
                        suppressed: Seq[Stacktrace])

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