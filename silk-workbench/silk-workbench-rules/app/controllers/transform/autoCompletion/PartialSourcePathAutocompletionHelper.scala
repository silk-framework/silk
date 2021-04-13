package controllers.transform.autoCompletion

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{DirectionalPathOperator, PathOperator, PathParser, UntypedPath}
import org.silkframework.util.StringUtils

object PartialSourcePathAutocompletionHelper {
  /**
    * Returns the part of the path that should be replaced and the extracted query words that can be used for search.
    * @param request   The partial source path auto-completion request payload.
    * @param openWorld If true, this is a
    * @param prefixes
    */
  def pathToReplace(request: PartialSourcePathAutoCompletionRequest,
                    openWorld: Boolean)
                   (implicit prefixes: Prefixes): PathToReplace = {
    val input = request.inputString
    val unfilteredQuery = Some(Seq.empty)
    if(input.isEmpty) {
      return PathToReplace(0, 0, unfilteredQuery)
    }
    val cursorPosition = request.cursorPosition
    val pathUntilCursor = input.take(cursorPosition)
    // If the cursor is placed in the middle of an operator expression, this contains the remaining characters after the cursor.
    val remainingCharsInOperator = input.substring(request.cursorPosition).takeWhile(c => !Set('/', '\\', '[').contains(c))
    val partialResult = UntypedPath.partialParse(pathUntilCursor)
    partialResult.error match {
      case Some(error) =>
        // TODO: Handle error cases, most likely inside filter expressions
        ???
      case None if partialResult.partialPath.operators.nonEmpty =>
        // No parse problem, use the last path segment (must be forward or backward path op) for auto-completion
        val lastPathOp = partialResult.partialPath.operators.last
        val extractedTextQuery = extractTextPart(lastPathOp)
        val fullQuery = extractedTextQuery.map(q => extractQuery(q + remainingCharsInOperator))
        if(extractedTextQuery.isEmpty) {
          // This is the end of a valid filter expression, do not replace or suggest anything besides default completions
          PathToReplace(pathUntilCursor.length, 0, None)
        } else if(lastPathOp.serialize.length >= pathUntilCursor.length) {
          // The path op is the complete input path
          PathToReplace(0, pathUntilCursor.length + remainingCharsInOperator.length, fullQuery)
        } else {
          // Replace the last path operator of the input path
          val lastOpLength = lastPathOp.serialize.length
          PathToReplace(cursorPosition - lastOpLength, lastOpLength + remainingCharsInOperator.length, fullQuery)
        }
      case None =>
        // Should never come so far
        PathToReplace(0, 0, unfilteredQuery)
    }
  }

  private def extractTextPart(pathOp: PathOperator): Option[String] = {
    pathOp match {
      case op: DirectionalPathOperator =>
        Some(op.property.uri)
      case _ =>
        // This is the end of a complete filter expression, suggest nothing to replace it with.
        None
    }
  }

  private def extractQuery(input: String): Seq[String] = {
    StringUtils.extractSearchTerms(input)
  }
}

/**
  * The part of the input path that should be replaced.
  * @param from   The start index of the substring that should be replaced.
  * @param length The length in characters of the string that should be replaced.
  * @param query  Extracted query as multi-word sequence, from the position of the cursor.
  *               If it is None this means that no query should be asked to find suggestions, i.e. only suggest operator or nothing.
  */
case class PathToReplace(from: Int, length: Int, query: Option[Seq[String]])