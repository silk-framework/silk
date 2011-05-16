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

    //?a rdf:type dbpedia:Settlement

//    Restriction.empty
  }


  def parser: Parser[Restriction] = tripelPatterns ^^ { r => Restriction(Some(r)) }

  //TODO
  /*
  regex für:
  beliebig viel whitespace + beliebig viele { + beliebig viel whitespace + triplePatterns + } + belebig viel whitepsace  + Union als String + ...
   */
  def unionPattern : Parser[Operator] = rep( anyWhitespace ~> tripelPatterns ~ "UNION" )

  //one or more whitespace
  def anyWhitespace = """ \s+ """.r

  // one or more curly brace
  def brace =

  def tripelPatterns: Parser[Operator] = rep( triplePattern <~ opt(".") ) ^^ { r => r match
  {
    case condition :: Nil => condition
    case head :: tail => And(r)
  }}

  override val skipWhitespace = false

  def triplePattern = subj ~ predicateObjectFilter ~ predicateObjectFilter  ^^
    { case v ~ p ~ o => Condition.resolve(Path.parse("?" + v + "/" + " " + p), Set(o)) }

  def subj = "?" ~> varAB ^^ { v => v }

  def varAB = "a" | "b"

  def predicateObjectFilter = " " ~> wordCharFilter ~ ":" ~ wordCharFilter ^^ { case prefix ~ ":" ~ name => prefix + ":" + name}

  def wordCharFilter = """[a-zA-Z_]\w*""".r

  //def end = predicateObjectFilter ~> pointFilter ^^ { e => e }


}


