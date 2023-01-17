package controllers.transform.autoCompletion

import controllers.autoCompletion.CompletionBase
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import play.api.libs.json.{Format, Json}

/** Requests that are sent by the auto-suggestion component. */
trait AutoSuggestAutoCompletionRequest {
  // The input string
  def inputString: String

  // The cursor position inside the input string
  def cursorPosition: Int

  // The optional number of max. suggestions that should be returned
  def maxSuggestions: Option[Int]

  /** The path until the cursor position. */
  def pathUntilCursor: String = inputString.take(cursorPosition)

  /** The character right before the cursor. */
  def charBeforeCursor: Option[Char] = pathUntilCursor.reverse.headOption
}

/**
  * The response for a auto-suggestion request that can be interpreted by the auto-suggestion UI component.
  *
  * @param inputString    The input string from the request for validation.
  * @param cursorPosition The cursor position from the request for validation.
  */
case class AutoSuggestAutoCompletionResponse(inputString: String,
                                             cursorPosition: Int,
                                             @ArraySchema(schema = new Schema(implementation = classOf[ReplacementResults]))
                                             replacementResults: Seq[ReplacementResults])

/**
  * Suggested replacement for a specific part of the input string.
  *
  * @param replacementInterval An optional interval if there has been found a part of the source path that can be replaced.
  * @param replacements        The auto-completion results.
  * @param extractedQuery      A query that has been extracted from around the cursor position that was used for the fil;tering of results.
  */
case class ReplacementResults(replacementInterval: ReplacementInterval,
                              extractedQuery: String,
                              @ArraySchema(schema = new Schema(implementation = classOf[CompletionBase]))
                              replacements: Seq[CompletionBase])

/** The part of a string to replace.
  *
  * @param from   The start index of the string to be replaced.
  * @param length The length in characters that should be replaced.
  */
case class ReplacementInterval(from: Int, length: Int)

object AutoSuggestAutoCompletionResponse {
  implicit val ReplacementIntervalFormat: Format[ReplacementInterval] = Json.format[ReplacementInterval]
  implicit val ReplacementResultsFormat: Format[ReplacementResults] = Json.format[ReplacementResults]
  implicit val autoSuggestAutoCompletionResponseFormat: Format[AutoSuggestAutoCompletionResponse] = Json.format[AutoSuggestAutoCompletionResponse]
}