package de.fuberlin.wiwiss.silk.util.convert


import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.util.ValidationException
import de.fuberlin.wiwiss.silk.instance.{Path, Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.config.Prefixes
import util.parsing.input.CharSequenceReader

import de.fuberlin.wiwiss.silk.instance.{Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.instance.Restriction.{Operator, Or, Condition, And}


/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
class RestrictionConverter(implicit prefixes : Prefixes) extends RegexParsers
{

  def apply(subjectVar : String, sparqlRestriction : SparqlRestriction) : Restriction =
  {
    parseAll(parser, new CharSequenceReader(sparqlRestriction.toString)) match {
      case Success(parsedPath, _) => parsedPath
      case error : NoSuccess => throw new ValidationException(error.toString)
  }
  }

   override val skipWhitespace = false

//  def parser: Parser[Restriction] = triplePatterns ^^ { r => Restriction(Some(r)) }

  def parser: Parser[Restriction] = unionPatterns ^^ { r => Restriction(Some(r)) }

  def unionPatterns : Parser[Operator] = rep(unionPattern <~ opt("UNION" <~ spaceOrNewLine) <~ opt(".")) ^^ { r => r match
    {
      case condition :: Nil => condition
      case head :: tail => Or(r)
    }}

  def unionPattern = (( anyWhitespace ~> brace) ~> anyWhitespace) ~> triplePatterns <~ (anyWhitespace <~ brace <~ spaceOrNewLine) ^^
  {
    case patterns => patterns
  }

  //one or more whitespace
  def anyWhitespace = """\s*""".r

  //one or more whitespace or newline
  def spaceOrNewLine = """[\s\n]*""".r

  // curly brace
  def brace = "{" | "}"

  def triplePatterns : Parser[Operator] = rep( triplePattern <~ opt(".") ) ^^ { r => r match
  {
    case condition :: Nil => condition
    case head :: tail => And(r)
  }}


  def triplePattern = subj ~ predicateObjectFilter ~ predicateObjectFilter  ^^
    { case v ~ p ~ o => Condition.resolve(Path.parse("?" + v + "/" + " " + p), Set(o)) }

  def subj = "?" ~> varAB ^^ { v => v }

  def varAB = "a" | "b"

  def predicateObjectFilter = " " ~> wordCharFilter ~ ":" ~ wordCharFilter ^^ { case prefix ~ ":" ~ name => prefix + ":" + name}

  def wordCharFilter = """[a-zA-Z_]\w*""".r


}


