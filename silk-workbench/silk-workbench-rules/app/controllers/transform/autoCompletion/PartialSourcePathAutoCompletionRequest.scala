package controllers.transform.autoCompletion

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.entity.paths.PathPositionStatus
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
  def pathUntilCursor: String = inputString.take(cursorPosition)
  def charBeforeCursor: Option[Char] = pathUntilCursor.reverse.headOption

  private val operatorStartChars = Set('/', '\\', '[')

  /** The remaining characters from the cursor position to the end of the current path operator. */
  def remainingStringInOperator: String = {
    val positionStatus = cursorPositionStatus
    inputString
      .substring(cursorPosition)
      .takeWhile { char =>
        positionStatus.update(char)
        positionStatus.insideQuotesOrUri || !operatorStartChars.contains(char)
      }
  }

  /** The full string of original representation of the path operator the cursor is currently placed in. */
  def currentOperatorString: String = {
    // Zero if in first path segment with stripped forward operator
    val operatorIdx = pathOperatorIdxBeforeCursor.getOrElse(0)
    inputString.substring(operatorIdx, indexOfOperatorEnd)
  }

  // True if the cursor is placed inside a backward operator
  def isInBackwardOp: Boolean = {
    currentOperatorString.startsWith("\\")
  }

  // True if cursor is placed inside an explicit forward operator, i.e. the / is written out and not implicit.
  def isInExplicitForwardOp: Boolean = {
    currentOperatorString.startsWith("/")
  }

  /** The string in the current operator up to the cursor. */
  def stringInOperatorToCursor: String = {
    inputString.substring(pathOperatorIdxBeforeCursor.getOrElse(0), cursorPosition)
  }

  /** Index of the path operator before the cursor.*/
  def pathOperatorIdxBeforeCursor: Option[Int] = {
    val positionStatus = cursorPositionStatus
    val strBackToOperator = inputString.substring(0, cursorPosition).reverse
      .takeWhile { char =>
        // Reverse open and close brackets of URI since we are going backwards
        val reversedChar = if(char == '<') '>' else if(char == '>') '<' else char
        positionStatus.update(reversedChar)
        positionStatus.insideQuotesOrUri || !operatorStartChars.contains(char)
      }
    Some(cursorPosition - strBackToOperator.length - 1).filter(_ >= 0)
  }

  // Checks if the cursor position is inside quotes or URI
  def cursorPositionStatus: PathPositionStatus = {
    val positionStatus = PathPositionStatus()
    inputString.take(cursorPosition).foreach(positionStatus.update)
    positionStatus
  }

  /** The index of the operator end, i.e. index in the input string from the cursor to the end of the current operator. */
  def indexOfOperatorEnd: Int = {
    cursorPosition + remainingStringInOperator.length
  }
}

object PartialSourcePathAutoCompletionRequest {
  implicit val partialSourcePathAutoCompletionRequestFormat: Format[PartialSourcePathAutoCompletionRequest] = Json.format[PartialSourcePathAutoCompletionRequest]
}

/**
  * The response for a partial source path auto-completion request.
  * @param inputString         The input string from the request for validation.
  * @param cursorPosition      The cursor position from the request for validation.
  */
case class PartialSourcePathAutoCompletionResponse(inputString: String,
                                                   cursorPosition: Int,
                                                   @ArraySchema(schema = new Schema(implementation = classOf[ReplacementResults]))
                                                   replacementResults: Seq[ReplacementResults])

/**
  * Suggested replacement for a specific part of the input string.
  *
  * @param replacementInterval An optional interval if there has been found a part of the source path that can be replaced.
  * @param replacements  The auto-completion results.
  * @param extractedQuery A query that has been extracted from around the cursor position that was used for the fil;tering of results.
  */
case class ReplacementResults(replacementInterval: ReplacementInterval,
                              extractedQuery: String,
                              @ArraySchema(schema = new Schema(implementation = classOf[CompletionBase]))
                              replacements: Seq[CompletionBase])

/** The part of a string to replace.
  *
  * @param from The start index of the string to be replaced.
  * @param length The length in characters that should be replaced.
  */
case class ReplacementInterval(from: Int, length: Int)

object PartialSourcePathAutoCompletionResponse {
  implicit val ReplacementIntervalFormat: Format[ReplacementInterval] = Json.format[ReplacementInterval]
  implicit val ReplacementResultsFormat: Format[ReplacementResults] = Json.format[ReplacementResults]
  implicit val partialSourcePathAutoCompletionResponseFormat: Format[PartialSourcePathAutoCompletionResponse] = Json.format[PartialSourcePathAutoCompletionResponse]
}

