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

package org.silkframework.entity.rdf

import org.silkframework.config.Prefixes
import org.silkframework.entity.Restriction.{And, Condition, Operator, Or}
import org.silkframework.entity.{Path, Restriction}
import org.silkframework.runtime.serialization.ValidationException
import org.silkframework.util.Uri

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader


/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
class SparqlRestrictionParser(implicit prefixes: Prefixes) extends RegexParsers {

  def apply(sparqlRestriction: SparqlRestriction): Restriction = {
    apply(sparqlRestriction.toString)
  }

  def apply(sparqlRestriction: String): Restriction = {
    // Check if pattern is empty
    val ignored = ".{}".toSet
    val isEmpty = sparqlRestriction.filterNot(ignored).trim.isEmpty
    if(isEmpty) {
      // Pattern is empty
      Restriction.empty
    } else {
      // Parse nonempty pattern
      parseAll(parser, new CharSequenceReader(sparqlRestriction)) match {
        case Success(parsedPath, _) => parsedPath
        case error: NoSuccess => throw new ValidationException(error.toString)
      }
    }
  }

  override val skipWhitespace = false

  def parser: Parser[Restriction] = unionPatterns ^^ {
    r => Restriction(Some(r))
  }

  def unionPatterns: Parser[Operator] = rep1(unionPattern <~ opt("UNION" <~ anyWhitespace) <~ opt(".")) ^^ {
    case operator :: Nil => operator
    case operators => Or(operators)
  }

  def unionPattern = (anyWhitespace ~> opt(repsep(fowbrace, anyWhitespace)) ~> anyWhitespace) ~> triplePatterns <~ (anyWhitespace <~ opt(repsep(revbrace, anyWhitespace)) <~ anyWhitespace) ^^ {
    case patterns => patterns
  }

  //one or more whitespace
  def anyWhitespace = """\s*""".r

  // curly brace forward
  def fowbrace = """\{+""".r

  // curly brace reward
  def revbrace = """\}+""".r

  def triplePatterns: Parser[Operator] = rep1(triplePattern <~ anyWhitespace <~ opt(".")) ^^ {
    case condition :: Nil => condition
    case conditions => And(conditions)
  }

  def triplePattern = subject ~ predicate ~ objectt ^^ {
    case v ~ p ~ o => Condition(Path.parse("?" + v + "/" + p), Uri.parse(o, prefixes).toString)
  }

  def subject = "?" ~> idChars ^^ {
    v => v
  }

  def predicate = " " ~> (prefixName | uri | rdfTypeReplacement)

  def objectt = " " ~> (variable | prefixName | uri)

  def variable = "?" ~> idChars ^^ {
    case name => ""
  }

  def uri = "<" ~> uriChars <~ ">" ^^ {
    case uri => "<" + uri + ">"
  }

  def prefixName = idChars ~ ":" ~ idChars ^^ {
    case prefix ~ ":" ~ name => prefix + ":" + name
  }

  def rdfTypeReplacement = "a" ^^ {
    _ => "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"
  }

  val idChars = """[a-zA-Z_]\w*""".r

  val uriChars = """[^>]+""".r
}


