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

package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.runtime.serialization.ValidationException

import util.parsing.input.CharSequenceReader
import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.util.Uri
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Parser for the Silk RDF path language.
 */
private class PathParser(prefixes: Prefixes) extends RegexParsers {

  private val defaultVar = "a"

  def parse(pathStr: String): Path = {
    if(pathStr.isEmpty) {
      Path(defaultVar, Nil)
    } else {
      // Complete path if a simplified syntax is used
      val completePath = pathStr.head match {
        case '?' => pathStr // Path is already complete
        case '/' | '\\' => "?" + defaultVar + pathStr // Variable has been left out
        case _ => "?a/" + pathStr // Variable and leading '/' have been left out
      }
      // Parse path
      parseAll(path, new CharSequenceReader(completePath)) match {
        case Success(parsedPath, _) => parsedPath
        case error: NoSuccess => throw new ValidationException(error.toString)
      }
    }
  }

  private def path = variable ~ rep(forwardOperator | backwardOperator | filterOperator) ^^ {
    case variable ~ operators => Path(variable, operators)
  }

  private def variable = "?" ~> identifier

  private def forwardOperator = "/" ~> identifier ^^ {
    s => ForwardOperator(Uri.parse(s, prefixes))
  }

  private def backwardOperator = "\\" ~> identifier ^^ {
    s => BackwardOperator(Uri.parse(s, prefixes))
  }

  private def filterOperator = "[" ~> (langFilter | propFilter) <~ "]"

  private def langFilter = "@lang" ~> compOperator ~ identifier ^^ {
    case op ~ lang => LanguageFilter(op, lang)
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

  // A value that is either an identifier or a literal value enclosed in quotes (e.g., "literal").
  private def value = identifier | "\"[^\"]+\"".r

  // A comparison operator
  private def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}
