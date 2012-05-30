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

import util.parsing.input.CharSequenceReader
import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.util.{ValidationException, Uri}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Parser for the Silk RDF path language.
 */
private class PathParser(prefixes: Prefixes) extends RegexParsers {
  def parse(pathStr: String): Path = {
    parseAll(path, new CharSequenceReader(pathStr)) match {
      case Success(parsedPath, _) => parsedPath
      case error: NoSuccess => throw new ValidationException(error.toString)
    }
  }

  private def path = variable ~ rep(forwardOperator | backwardOperator | filterOperator) ^^ {
    case variable ~ operators => Path(variable, operators)
  }

  private def variable = "?" ~> literal

  private def forwardOperator = "/" ~> literal ^^ {
    s => ForwardOperator(Uri.parse(s, prefixes))
  }

  private def backwardOperator = "\\" ~> literal ^^ {
    s => BackwardOperator(Uri.parse(s, prefixes))
  }

  private def filterOperator = "[" ~> (langFilter | propFilter) <~ "]"

  private def langFilter = "@lang" ~> compOperator ~ literal ^^ {
    case op ~ lang => LanguageFilter(op, lang)
  }

  private def propFilter = literal ~ compOperator ~ literal ^^ {
    case prop ~ op ~ value => PropertyFilter(Uri.parse(prop, prefixes).uri, op, value)
  }

  private def literal = """<[^>]+>|[^\\/\[\] ]+""".r

  private def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}
