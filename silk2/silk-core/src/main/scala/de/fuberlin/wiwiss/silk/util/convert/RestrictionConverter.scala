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

  def unionPatterns : Parser[Operator] = rep1(unionPattern <~ opt("UNION" <~ anyWhitespace) <~ opt(".")) ^^ { r => r match
    {
      case operator :: Nil => operator
      case operators => Or(operators)
    }}

  def unionPattern = ( anyWhitespace ~> opt(repsep(fowbrace,anyWhitespace)) ~> anyWhitespace) ~> triplePatterns <~ (anyWhitespace <~ opt(repsep(revbrace, anyWhitespace)) <~ anyWhitespace)   ^^
  {
    case patterns => patterns
  }

  //one or more whitespace
  def anyWhitespace = """\s*""".r

  // curly brace forward
  def fowbrace = """\{+""".r

// curly brace reward
  def revbrace = """\}+""".r


  def triplePatterns : Parser[Operator] = rep1( triplePattern <~ anyWhitespace <~ opt(".") ) ^^ { r  => r match
  {
    case condition :: Nil => condition
    case conditions => And(conditions)
  }}

  def triplePattern = subj ~ predicate ~ objectt  ^^
    { case v ~ p ~ o => Condition.resolve(Path.parse("?" + v + "/" + " " + p), o) }

  def subj = "?" ~> wordCharFilter ^^ { v => v }

  def predicate = predicateObjectFilter | rdfTypeReplacement

  def predicateObjectFilter = " " ~> wordCharFilter ~ ":" ~ wordCharFilter ^^ { case prefix ~ ":" ~ name => Set(prefix + ":" + name)}

  def rdfTypeReplacement = " a" ^^ { rdftype => "rdf:type" }

  def objectt = specialObj | predicateObjectFilter

  def specialObj = " " ~> "?" ~ wordCharFilter ^^ { case "?" ~ name => Set[String]() }

  def wordCharFilter = """[a-zA-Z_]\w*""".r

}


