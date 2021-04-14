package controllers.transform.autoCompletion

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{DirectionalPathOperator, PartialParseResult, PathOperator, PathParser, UntypedPath}
import org.silkframework.util.StringUtils

object PartialSourcePathAutocompletionHelper {
  /**
    * Returns the part of the path that should be replaced and the extracted query words that can be used for search.
    * @param request   The partial source path auto-completion request payload.
    * @param subPathOnly If true, only a sub-part of the path is replaced, else a path suffix
    */
  def pathToReplace(request: PartialSourcePathAutoCompletionRequest,
                    subPathOnly: Boolean)
                   (implicit prefixes: Prefixes): PathToReplace = {
    val unfilteredQuery: Option[String] = Some("")
    if(request.inputString.isEmpty) {
      return PathToReplace(0, 0, unfilteredQuery)
    }
    val partialResult = UntypedPath.partialParse(request.pathUntilCursor)
    val replacement = partialResult.error match {
      case Some(error) =>
        val errorOffsetCharacter = request.inputString.substring(error.offset, error.offset + 1)
        val parseStartCharacter = if(error.inputLeadingToError.isEmpty) errorOffsetCharacter else error.inputLeadingToError.take(1)
        if(error.inputLeadingToError.startsWith("[")) {
          // Error happened inside of a filter
          // TODO: Find out where in the filter we are and try to auto-complete the correct thing
          PathToReplace(0, 0, unfilteredQuery)
        } else if(parseStartCharacter == "/" || parseStartCharacter == "\\") {
          // It tried to parse a forward or backward path and failed, replace path and use path value as query
          val operatorValue = request.inputString.substring(error.offset + 1, request.cursorPosition) + request.remainingStringInOperator
          PathToReplace(error.offset, operatorValue.length + 1, Some(extractQuery(operatorValue)))
        } else {
          // The parser parsed part of a forward or backward path as a path op and then failed on an invalid char, e.g. "/with space"
          // parses "with" as forward op and then fails parsing the space.
          assert(partialResult.partialPath.operators.nonEmpty, "Could not detect sub-path to be replaced.")
          handleMiddleOfPathOp(partialResult, request)
        }
      case None if partialResult.partialPath.operators.nonEmpty =>
        // No parse problem, use the last path segment (must be forward or backward path op) for auto-completion
        handleMiddleOfPathOp(partialResult, request)
      case None =>
        // Should never come so far
        PathToReplace(0, 0, unfilteredQuery)
    }
    handleSubPathOnly(request, replacement, subPathOnly)
  }

  private def handleSubPathOnly(request: PartialSourcePathAutoCompletionRequest, pathToReplace: PathToReplace, subPathOnly: Boolean): PathToReplace = {
    if(subPathOnly || pathToReplace.query.isEmpty) {
      pathToReplace
    } else {
      pathToReplace.copy(length = request.inputString.length - pathToReplace.from)
    }
  }

  private def handleMiddleOfPathOp(partialResult: PartialParseResult,
                                   request: PartialSourcePathAutoCompletionRequest): PathToReplace = {
    val lastPathOp = partialResult.partialPath.operators.last
    val extractedTextQuery = extractTextPart(lastPathOp)
    val fullQuery = extractedTextQuery.map(q => extractQuery(q + request.remainingStringInOperator))
    if(extractedTextQuery.isEmpty) {
      // This is the end of a valid filter expression, do not replace or suggest anything besides default completions
      PathToReplace(request.pathUntilCursor.length, 0, None)
    } else if(lastPathOp.serialize.length >= request.pathUntilCursor.length) {
      // The path op is the complete input path
      PathToReplace(0, request.pathUntilCursor.length + request.remainingStringInOperator.length, fullQuery)
    } else {
      // Replace the last path operator of the input path
      val lastOpLength = lastPathOp.serialize.length
      PathToReplace(request.cursorPosition - lastOpLength, lastOpLength + request.remainingStringInOperator.length, fullQuery)
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

  /** Grammar taken from the Turtle EBNF */
  private val PN_CHARS_BASE = """[A-Za-z\x{00C0}-\x{00D6}\x{00D8}-\x{00F6}\x{00F8}-\x{02FF}\x{0370}-\x{037D}\x{037F}-\x{1FFF}\x{200C}-\x{200D}\x{2070}-\x{218F}\x{2C00}-\x{2FEF}\x{3001}-\x{D7FF}\x{F900}-\x{FDCF}\x{FDF0}-\x{FFFD}\x{10000}-\x{EFFFF}]"""
  private val PN_CHARS_U = s"""$PN_CHARS_BASE|_"""
  private val PN_CHARS = s"""$PN_CHARS_U|-|[0-9]|\\x{00B7}|[\\x{0300}-\\x{036F}]|[\\x{203F}-\\x{2040}]"""
  private val prefixRegex = s"""$PN_CHARS_BASE(($PN_CHARS|\\.)*$PN_CHARS)?"""
  private val startsWithPrefix = s"""^$prefixRegex:""".r

  private def extractQuery(input: String): String = {
    var inputToProcess: String = input
    if(!input.contains("<") && input.contains(":") && !input.contains(" ") && startsWithPrefix.findFirstMatchIn(input).isDefined) {
      // heuristic to detect qualified names
      inputToProcess = input.drop(input.indexOf(":") + 1)
    }
    StringUtils.extractSearchTerms(inputToProcess).mkString(" ")
  }
}

/**
  * The part of the input path that should be replaced.
  * @param from   The start index of the substring that should be replaced.
  * @param length The length in characters of the string that should be replaced.
  * @param query  Extracted query from the characters around the position of the cursor.
  *               If it is None this means that no query should be asked to find suggestions, i.e. only suggest operator or nothing.
  */
case class PathToReplace(from: Int, length: Int, query: Option[String])