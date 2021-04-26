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

  class PositionStatus(initialInsideDoubleQuotes: Boolean,
                       initialInsideSingleQuotes: Boolean,
                       initialInsideUri: Boolean,
                       initialInsideFilter: Boolean) {
    private var _insideDoubleQuotes = initialInsideDoubleQuotes
    private var _insideSingleQuotes = initialInsideSingleQuotes
    private var _insideUri = initialInsideUri
    private var _insideFilter = initialInsideFilter

    def update(char: Char): Unit = {
      // Track quotes status
      if (!insideUri) {
        if (char == '"' && !_insideSingleQuotes) {
          _insideDoubleQuotes = !_insideDoubleQuotes
        }
        if(char == '\'' && !_insideDoubleQuotes) {
          _insideSingleQuotes = !_insideSingleQuotes
        }
      }
      // Track URI status
      if (!insideQuotes) {
        if (char == '<') {
          _insideUri = true
        } else if (char == '>') {
          _insideUri = false
        }
      }
      // Track filter status
      if (!insideQuotes && !insideUri) {
        if (char == '[') {
          _insideFilter = true
        } else if (char == ']') {
          _insideFilter = false
        }
      }
      (insideQuotes, insideUri)
    }

    def insideQuotes: Boolean = _insideDoubleQuotes || _insideSingleQuotes
    def insideUri: Boolean = _insideUri
    def insideFilter: Boolean = _insideFilter
    def insideQuotesOrUri: Boolean = insideQuotes || insideUri
  }

  // Checks if the cursor position is inside quotes or URI
  def cursorPositionStatus: PositionStatus = {
    val positionStatus = new PositionStatus(false, false, false, false)
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

