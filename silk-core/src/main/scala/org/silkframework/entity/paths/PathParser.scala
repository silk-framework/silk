/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

/**
 * Parser for the Silk RDF path language.
 */
private[entity] class PathParser(prefixes: Prefixes) extends RegexParsers {

  def parse(pathStr: String): UntypedPath = {
    if(pathStr.isEmpty) {
      UntypedPath(Nil)
    } else {
      val completePath = normalized(pathStr)
      // Parse path
      parseAll(path, new CharSequenceReader(completePath)) match {
        case Success(parsedPath, _) => parsedPath
        case error: NoSuccess => throw new ValidationException(error.toString)
      }
    }
  }

  /**
    * Returns the part of the path that could be parsed until a parse error and the error if one occurred.
    * @param pathStr The input path string.
    */
  def parseUntilError(pathStr: String): PartialParseResult = {
    val completePath = normalized(pathStr)
    // Added characters because of normalization. Need to be removed when reporting the actual error offset.
    val addedOffset = completePath.length - pathStr.length
    val inputSequence = new CharSequenceReader(completePath)
    var partialPathOps: Vector[PathOperator] = Vector.empty
    var partialParseError: Option[PartialParseError] = None
    val variableResult = parse(variable, inputSequence) // Ignore variable
    var parseOffset = variableResult.next.offset
    def originalParseOffset = math.max(0, parseOffset - addedOffset)
    while(partialParseError.isEmpty && parseOffset < completePath.length) {
      try {
        parse(ops, inputSequence.drop(parseOffset)) match {
          case Success(pathOperator, next) => {
            partialPathOps :+= pathOperator
            parseOffset = next.offset
          }
          case error: NoSuccess =>
            // Subtract 1 because next is positioned after the character that lead to the parse error.
            val originalErrorOffset = math.max(error.next.offset - addedOffset - 1, 0)
            partialParseError = Some(PartialParseError(
              originalErrorOffset,
              error.msg,
              pathStr.substring(originalParseOffset, originalErrorOffset + 1) // + 1 since we want to have the character where it failed
            ))
        }
      } catch {
        case validationException: ValidationException =>
          // Can happen e.g. when a qualified name used an invalid/unknown prefix name
          partialParseError = Some(PartialParseError(
            originalParseOffset,
            validationException.getMessage,
            ""
          ))
      }
    }
    PartialParseResult(UntypedPath(partialPathOps.toList), partialParseError)
  }

  // Normalizes the path syntax in case a simplified syntax has been used
  private def normalized(pathStr: String): String = {
    pathStr.head match {
      case '?' => pathStr // Path includes a variable
      case '/' | '\\' => "?a" + pathStr // Variable has been left out
      case _ => "?a/" + pathStr // Variable and leading '/' have been left out
    }
  }

  private def ops = forwardOperator | backwardOperator | filterOperator ^^ {
    case operator => operator
  }

  private def path = variable ~ rep(ops) ^^ {
    case variable ~ operators => UntypedPath(operators)
  }

  private def variable = "?" ~> identifier

  private def forwardOperator = "/" ~> identifier ^^ {
    s => ForwardOperator(Uri.parse(s, prefixes).uri)
  }

  private def backwardOperator = "\\" ~> identifier ^^ {
    s => BackwardOperator(Uri.parse(s, prefixes).uri)
  }

  private def filterOperator = "[" ~> (langFilter | propFilter) <~ "]"

  private def langFilter = "@lang" ~> compOperator ~ "'" ~ languageTag ~ "'" ^^ {
    case op ~ _ ~ lang ~ _ => LanguageFilter(op, lang)
  }

  private def propFilter = identifier ~ compOperator ~ value ^^ {
    case prop ~ op ~ value =>
      PropertyFilter(
        property = Uri.parse(prop, prefixes),
        operator = op,
        value = if(value.startsWith("\"")) value else "<" + Uri.parse(value, prefixes).uri + ">")
  }

  // An identifier that is either a URI enclosed in angle brackets (e.g., <URI>) or a plain identifier (e.g., name or prefix:name)
  private def identifier = """<[^>]+>|[^\\/\[\]<>=!" ]+""".r

  // A language tag according to the Sparql spec
  private def languageTag = """[a-zA-Z]+('-'[a-zA-Z0-9]+)*""".r

  // A value that is either an identifier or a literal value enclosed in quotes (e.g., "literal").
  private def value = identifier | "\"[^\"]+\"".r

  // A comparison operator
  private def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}

/**
  * A partial path parse result.
  *
  * @param partialPath The (valid) partial path that has been parsed until the parse error.
  * @param error       An optional parse error when not all of the input string could be parsed.
  */
case class PartialParseResult(partialPath: UntypedPath, error: Option[PartialParseError])

/** Offset and error message of the parse error. The offset defines the position before the character that lead to the parse error. */
case class PartialParseError(offset: Int, message: String, inputLeadingToError: String)