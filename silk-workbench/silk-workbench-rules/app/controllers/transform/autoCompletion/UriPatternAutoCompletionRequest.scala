package controllers.transform.autoCompletion

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.rule.util.UriPatternParser.PathPart
import org.silkframework.rule.util.{UriPatternParser, UriPatternSegments}
import play.api.libs.json.{Format, Json}

@Schema(description = "Auto-completion request for a path expression inside a URI pattern.")
case class UriPatternAutoCompletionRequest(@Schema(description = "The input string that should be auto-completed.", required = true)
                                           inputString: String,
                                           @Schema(description = "The cursor position in the input string.", required = true)
                                           cursorPosition: Int,
                                           @Schema(description = "The max. number of suggestions to return.", required = true)
                                           maxSuggestions: Option[Int],
                                           @Schema(description = "An additional object path this auto-completion should be the context of.", required = false)
                                           objectPath: Option[String]) extends AutoSuggestAutoCompletionRequest {
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