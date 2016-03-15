package models

import org.silkframework.runtime.validation.ValidationIssue
import play.api.libs.json.Json

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
      "id" -> msg.id.map(_.toString).getOrElse("")
    )
  }

}
