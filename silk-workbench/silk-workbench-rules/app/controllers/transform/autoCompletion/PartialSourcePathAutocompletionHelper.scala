package controllers.transform.autoCompletion

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{DirectionalPathOperator, PartialParseError, PartialParseResult, PathOperator, PathParser, UntypedPath}
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
    if(request.cursorPositionStatus.insideQuotes) {
      // Do not auto-complete inside quotes
      return PathToReplace(request.cursorPosition, 0, None, insideQuotes = true)
    }
    val replacement = partialResult.error match {
      case Some(error) =>
        handleParseErrorCases(request, unfilteredQuery, partialResult, error)
      case None if partialResult.partialPath.operators.nonEmpty =>
        // No parse problem, use the last path segment (must be forward or backward path op) for auto-completion
        handleMiddleOfPathOp(partialResult, request)
      case None if partialResult.partialPath.operators.isEmpty =>
        // Cursor position is 0.
        if(operatorChars.contains(request.inputString.head)) {
          // Right before another path operator, propose to that a new path op
          PathToReplace(0, 0, unfilteredQuery)
        } else {
          // Replace the remaining string
          val query = request.inputString.substring(0, request.indexOfOperatorEnd)
          PathToReplace(0, request.indexOfOperatorEnd, Some(extractQuery(query)))
        }
      case None =>
        // Should never come so far
        PathToReplace(0, 0, unfilteredQuery)
    }
    handleSubPathOnly(request, replacement, subPathOnly)
  }

  val operatorChars = Set('/', '\\', '[')

  // Handles the cases where an error occurred parsing the path until the cursor position
  private def handleParseErrorCases(request: PartialSourcePathAutoCompletionRequest,
                                    unfilteredQuery: Option[String],
                                    partialResult: PartialParseResult, error: PartialParseError): PathToReplace = {
    val errorOffsetCharacter = request.inputString.substring(error.offset, error.offset + 1)
    val parseStartCharacter = if (error.inputLeadingToError.isEmpty) errorOffsetCharacter else error.inputLeadingToError.take(1)
    if(error.offset < request.pathOperatorIdxBeforeCursor.getOrElse(0)) {
      PathToReplace(request.cursorPosition, 0, None, insideQuotes = request.cursorPositionStatus.insideQuotes, insideUri = request.cursorPositionStatus.insideUri)
    } else if (error.inputLeadingToError.startsWith("[")) {
      handleFilter(request, unfilteredQuery, error)
    } else if (parseStartCharacter == "/" || parseStartCharacter == "\\") {
      // It tried to parse a forward or backward path and failed, replace path and use path value as query
      val operatorValue = request.inputString.substring(error.offset + 1, request.cursorPosition) + request.remainingStringInOperator
      PathToReplace(error.offset, operatorValue.length + 1, Some(extractQuery(operatorValue)))
    } else if (error.nextParseOffset == 0) {
      // It failed to parse the first path op, just take the whole string up to the next path operator as input to replace
      val operatorValue = request.inputString.substring(0, request.indexOfOperatorEnd)
      PathToReplace(0, operatorValue.length, Some(extractQuery(operatorValue)))
    } else {
      // The parser parsed part of a forward or backward path as a path op and then failed on an invalid char, e.g. "/with space"
      // parses "with" as forward op and then fails parsing the space.
      assert(partialResult.partialPath.operators.nonEmpty, "Could not detect sub-path to be replaced.")
      handleMiddleOfPathOp(partialResult, request)
    }
  }

  private def handleFilter(request: PartialSourcePathAutoCompletionRequest, unfilteredQuery: Option[String], error: PartialParseError): PathToReplace = {
    // Error happened inside of a filter
    // Characters that will usually end an identifier in a filter expression. For some URIs this could lead to false positives, e.g. that contain '='.
    val stopChars = Set('!', '=', '<', '>', ']')
    val pathFromFilter = request.inputString.substring(error.nextParseOffset)
    val insideFilterExpression = pathFromFilter.drop(1).trim
    if (insideFilterExpression.startsWith("@lang")) {
      // Not sure what to propose inside a lang filter
      PathToReplace(0, 0, None)
    } else {
      var identifier = ""
      if (insideFilterExpression.startsWith("<")) {
        // URI following
        val uriEndingIdx = insideFilterExpression.indexOf('>')
        if (uriEndingIdx > 0) {
          identifier = insideFilterExpression.take(uriEndingIdx + 1)
        } else {
          // URI is not closed, just take everything until either an operator or filter closing
          identifier = insideFilterExpression.take(1) + insideFilterExpression.drop(1).takeWhile(c => !stopChars.contains(c))
        }
      } else {
        identifier = insideFilterExpression.takeWhile(c => !stopChars.contains(c))
      }
      if(identifier.nonEmpty) {
        val pathFromFilterToCursor = request.inputString.substring(error.nextParseOffset, request.cursorPosition)
        if(pathFromFilterToCursor.endsWith(identifier)) {
          // The cursor is directly behind the identifier
          replaceIdentifierInsideFilter(error, identifier)
        } else if(pathFromFilterToCursor.contains(identifier)) {
          // The cursor is behind the identifier / URI TODO: auto-complete comparison operator
          PathToReplace(0, 0, None)
        } else {
          // Suggest to replace the identifier
          replaceIdentifierInsideFilter(error, identifier)
        }
      } else {
        replaceIdentifierInsideFilter(error, "")
      }
    }
  }

  private def replaceIdentifierInsideFilter(error: PartialParseError, identifier: String): PathToReplace = {
    PathToReplace(error.nextParseOffset + 1, identifier.stripSuffix(" ").length, Some(extractQuery(identifier)), insideFilter = true)
  }

  /** Replace the complete path prefix if not only the sub-path should be replaced, e.g. for XML and JSON. */
  private def handleSubPathOnly(request: PartialSourcePathAutoCompletionRequest,
                                pathToReplace: PathToReplace,
                                subPathOnly: Boolean): PathToReplace = {
    if(subPathOnly || pathToReplace.query.isEmpty || pathToReplace.insideFilter) {
      pathToReplace
    } else {
      pathToReplace.copy(length = request.inputString.length - pathToReplace.from)
    }
  }

  private def handleMiddleOfPathOp(partialResult: PartialParseResult,
                                   request: PartialSourcePathAutoCompletionRequest): PathToReplace = {
    val lastPathOp = partialResult.partialPath.operators.last
    val extractedTextQuery = extractTextPart(lastPathOp)
    val remainingStringInOp = partialResult.error match {
      case Some(error) => request.inputString.substring(error.nextParseOffset, request.indexOfOperatorEnd)
      case None => request.remainingStringInOperator
    }
    val fullQuery = extractedTextQuery.map(q => extractQuery(q + remainingStringInOp))
    if(extractedTextQuery.isEmpty) {
      // This is the end of a valid filter expression, do not replace or suggest anything besides default completions
      PathToReplace(request.pathUntilCursor.length, 0, None)
    } else if(lastPathOp.serialize.length >= request.pathUntilCursor.length) {
      // The path op is the complete input path
      PathToReplace(0, request.pathUntilCursor.length + request.remainingStringInOperator.length, fullQuery)
    } else {
      // Replace the last path operator of the input path
      val lastOpLength = lastPathOp.serialize.length
      val from = math.max(partialResult.error.map(_.nextParseOffset).getOrElse(request.cursorPosition) - lastOpLength, 0)
      PathToReplace(from, lastOpLength + request.remainingStringInOperator.length, fullQuery)
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
    var inputToProcess: String = input.trim
    if(input.startsWith("<") && input.endsWith(">")) {
      inputToProcess = input.drop(1).dropRight(1)
    } else if(!input.contains("<") && input.contains(":") && !input.contains(" ") && startsWithPrefix.findFirstMatchIn(input).isDefined
      && !input.startsWith("http") && !input.startsWith("urn")) {
      // heuristic to detect qualified names
      inputToProcess = input.drop(input.indexOf(":") + 1)
    }
    StringUtils.extractSearchTerms(inputToProcess).mkString(" ")
  }
}

/**
  * The part of the input path that should be replaced.
  *
  * @param from         The start index of the substring that should be replaced.
  * @param length       The length in characters of the string that should be replaced.
  * @param query        Extracted query from the characters around the position of the cursor.
  *                     If it is None this means that no query should be asked to find suggestions, i.e. only suggest operator or nothing.
  * @param insideFilter If the path to be replaced is inside a filter expression
  */
case class PathToReplace(from: Int, length: Int, query: Option[String], insideFilter: Boolean = false, insideQuotes: Boolean = false, insideUri: Boolean = false) {
  def insideQuotesOrUri: Boolean = insideQuotes || insideUri
}