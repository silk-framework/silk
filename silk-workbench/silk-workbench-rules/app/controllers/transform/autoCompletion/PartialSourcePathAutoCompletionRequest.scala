package controllers.transform.autoCompletion

import play.api.libs.json.{Format, Json}

/**
  * Request payload for partial source path auto-completion, i.e. suggest replacements for only parts of a more complex source path.
  *
  * @param inputString    The currently entered source path string.
  * @param cursorPosition The cursor position inside the source path string.
  * @param maxSuggestions The max. number of suggestions to return.
  */
case class PartialSourcePathAutoCompletionRequest(inputString: String,
                                                  cursorPosition: Int,
                                                  maxSuggestions: Option[Int]) {
  /** The path until the cursor position. */
  lazy val pathUntilCursor: String = inputString.take(cursorPosition)

  /** The remaining characters from the cursor position to the end of the current path operator. */
  lazy val remainingStringInOperator: String = {
    // TODO: This does not consider being in a literal string, i.e. "...^..." where /, \ and [ would be allowed
    val operatorStartChars = Set('/', '\\', '[')
    inputString
      .substring(cursorPosition)
      .takeWhile { char =>
        !operatorStartChars.contains(char)
      }
  }
}

object PartialSourcePathAutoCompletionRequest {
  implicit val partialSourcePathAutoCompletionRequestFormat: Format[PartialSourcePathAutoCompletionRequest] = Json.format[PartialSourcePathAutoCompletionRequest]
}

/**
  * The response for a partial source path auto-completion request.
  * @param inputString         The input string from the request for validation.
  * @param cursorPosition      The cursor position from the request for validation.
  * @param replacementInterval An optional interval if there has been found a part of the source path that can be replaced.
  * @param replacementResults  The auto-completion results.
  */
case class PartialSourcePathAutoCompletionResponse(inputString: String,
                                                   cursorPosition: Int,
                                                   replacementInterval: Option[ReplacementInterval],
                                                   extractedQuery: String,
                                                   replacementResults: CompletionsBase)

/** The part of a string to replace.
  *
  * @param from The start index of the string to be replaced.
  * @param length The length in characters that should be replaced.
  */
case class ReplacementInterval(from: Int, length: Int)

object PartialSourcePathAutoCompletionResponse {
  implicit val ReplacementIntervalFormat: Format[ReplacementInterval] = Json.format[ReplacementInterval]
  implicit val partialSourcePathAutoCompletionResponseFormat: Format[PartialSourcePathAutoCompletionResponse] = Json.format[PartialSourcePathAutoCompletionResponse]
}

