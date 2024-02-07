package controllers.transform.autoCompletion

import controllers.autoCompletion.AutoSuggestAutoCompletionRequest
import org.silkframework.entity.paths.PathPositionStatus
import org.silkframework.workspace.activity.workflow.WorkflowTaskContext
import play.api.libs.json.{Format, Json}

/**
  * Request payload for partial source path auto-completion, i.e. suggest replacements for only parts of a more complex source path.
  *
  * @param inputString    The currently entered source path string.
  * @param cursorPosition The cursor position inside the source path string.
  * @param maxSuggestions The max. number of suggestions to return.
  * @param isObjectPath   Set to true if the auto-completion results are meant for an object path. Some suggestions might be filtered out or added.
  */
case class PartialSourcePathAutoCompletionRequest(inputString: String,
                                                  cursorPosition: Int,
                                                  maxSuggestions: Option[Int],
                                                  isObjectPath: Option[Boolean],
                                                  taskContext: Option[WorkflowTaskContext]) extends AutoSuggestAutoCompletionRequest {
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