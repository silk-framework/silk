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

package de.fuberlin.wiwiss.silk.util.convert

import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.util.ValidationException
import de.fuberlin.wiwiss.silk.entity.Path
import util.parsing.input.CharSequenceReader

import de.fuberlin.wiwiss.silk.entity.{Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.Restriction.{Operator, Or, Condition, And}


/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
class RestrictionConverter(implicit prefixes: Prefixes) extends RegexParsers {

  def apply(subjectVar: String, sparqlRestriction: SparqlRestriction): Restriction = {
    parseAll(parser, new CharSequenceReader(sparqlRestriction.toString)) match {
      case Success(parsedPath, _) => parsedPath
      case error: NoSuccess => throw new ValidationException(error.toString)
    }
  }

  override val skipWhitespace = false

  //  def parser: Parser[Restriction] = triplePatterns ^^ { r => Restriction(Some(r)) }

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

  def triplePattern = subj ~ predicate ~ objectt ^^ {
    case v ~ p ~ o => Condition.resolve(Path.parse("?" + v + "/" + p.head), o)
  }

  def subj = "?" ~> wordCharFilter ^^ {
    v => v
  }

  def predicate = predicateObjectFilter | rdfTypeReplacement

  def predicateObjectFilter = " " ~> wordCharFilter ~ ":" ~ wordCharFilter ^^ {
    case prefix ~ ":" ~ name => Set(prefix + ":" + name)
  }

  def rdfTypeReplacement = " a" ^^ {
    rdftype => Set("rdf:type")
  }

  def objectt = specialObj | predicateObjectFilter

  def specialObj = " " ~> "?" ~ wordCharFilter ^^ {
    case "?" ~ name => Set[String]()
  }

  def wordCharFilter = """[a-zA-Z_]\w*""".r

}


