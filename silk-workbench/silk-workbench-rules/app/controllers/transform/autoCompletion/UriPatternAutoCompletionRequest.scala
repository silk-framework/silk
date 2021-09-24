package controllers.transform.autoCompletion

import org.silkframework.rule.util.UriPatternParser.PathPart
import org.silkframework.rule.util.{UriPatternParser, UriPatternSegments}
import play.api.libs.json.{Format, Json}

/** Request for auto-completion of path expressions inside URI patterns. */
case class UriPatternAutoCompletionRequest(inputString: String,
                                           cursorPosition: Int,
                                           maxSuggestions: Option[Int]) extends AutoSuggestAutoCompletionRequest {
  lazy val pathSegments: UriPatternSegments = UriPatternParser.parseIntoSegments(inputString, allowIncompletePattern = true)
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