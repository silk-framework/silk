package controllers.transform.autoCompletion

import controllers.autoCompletion.AutoSuggestAutoCompletionRequest
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.rule.util.UriPatternParser.PathPart
import org.silkframework.rule.util.{UriPatternParser, UriPatternSegments}
import org.silkframework.workspace.activity.workflow.WorkflowTaskContext
import play.api.libs.json.{Format, Json}

@Schema(description = "Auto-completion request for a path expression inside a URI pattern.")
case class UriPatternAutoCompletionRequest(@Schema(description = "The input string that should be auto-completed.", requiredMode = RequiredMode.REQUIRED)
                                           inputString: String,
                                           @Schema(description = "The cursor position in the input string.", requiredMode = RequiredMode.REQUIRED)
                                           cursorPosition: Int,
                                           @Schema(description = "The max. number of suggestions to return.", requiredMode = RequiredMode.REQUIRED)
                                           maxSuggestions: Option[Int],
                                           @Schema(description = "An additional object path this auto-completion should be the context of.", requiredMode = RequiredMode.NOT_REQUIRED)
                                           objectPath: Option[String],
                                          @Schema(description = "The workflow context a project task is executed in.", requiredMode = RequiredMode.NOT_REQUIRED)
                                           workflowTaskContext: Option[WorkflowTaskContext]) extends AutoSuggestAutoCompletionRequest {
  private lazy val pathSegments: UriPatternSegments = UriPatternParser.parseIntoSegments(inputString, allowIncompletePattern = true)
  lazy val activePathPart: Option[PathPart] = {
    pathSegments.segments.find(segment => cursorPosition <= segment.segmentPosition.originalEndIndex) match {
      case Some(pathPart @ PathPart(_, position)) if position.originalStartIndex <= cursorPosition =>
        Some(pathPart)
      case _ =>
        // Not in a path expression
        None
    }
  }

  def insidePathExpression(): Boolean = {
    activePathPart.isDefined
  }
}

object UriPatternAutoCompletionRequest {
  implicit val uriPatternAutoCompletionRequestFormat: Format[UriPatternAutoCompletionRequest] = Json.format[UriPatternAutoCompletionRequest]
}