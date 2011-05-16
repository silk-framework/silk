package de.fuberlin.wiwiss.silk.util.convert

import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.util.ValidationException
import de.fuberlin.wiwiss.silk.instance.Restriction.{Or, Condition, And}
import de.fuberlin.wiwiss.silk.instance.{Path, Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.config.Prefixes
import util.parsing.input.CharSequenceReader

/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
object RestrictionConverter extends RegexParsers
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

    def parser: Parser[Restriction] = triplePattern ^^ { r => Restriction(Some(r)) }

    override val skipWhitespace = false

    def triplePattern = subj ~ predicateObjectFilter ~ predicateObjectFilter ^^
      { case v ~ p ~ o =>
        Condition(Path.parse("?" + v + "/" + " " + p),
          Set(o)) }

    def subj = "?" ~> varAB ^^ { v => v }

    def varAB = "a" | "b"

  //TODO
//    def predicateObjectFilter = " " ~> wordCharFilter ^^ { case " " ~ name => " "+name }
//  def predicateObjectFilter = " " ~> wordCharFilter ~ (":" ~> wordCharFilter) ^^ { case prefix ~ name => prefix + ":" + name }
//  def predicateObjectFilter = " " ~ wordCharFilter ~ ":" ~ wordCharFilter ^^ { case " " ~ prefix ~ ":" ~ name => " " + prefix + ":" + name}
    def predicateObjectFilter = " " ~> wordCharFilter ~ ":" ~ wordCharFilter ^^ { case prefix ~ ":" ~ name => prefix + ":" + name}

    def wordCharFilter = """[a-zA-Z_]\w*""".r
}                                       //\p{Punct}