package de.fuberlin.wiwiss.silk.instance

import util.parsing.input.CharSequenceReader
import util.parsing.combinator.RegexParsers
import de.fuberlin.wiwiss.silk.config.ValidationException
import de.fuberlin.wiwiss.silk.util.Uri

/**
 * Parser for the Silk RDF path language.
 */
private class PathParser(prefixes : Map[String, String]) extends RegexParsers
{
  def parse(pathStr : String) : Path =
  {
    parseAll(path, new CharSequenceReader(pathStr)) match
    {
      case Success(parsedPath, _) => parsedPath
      case error : NoSuccess => throw new ValidationException(error.toString)
    }
  }

  private def path = variable ~ rep(forwardOperator | backwardOperator | filterOperator) ^^ { case variable ~ operators => Path(variable, operators) }

  private def variable = "?" ~> literal
  private def forwardOperator = "/" ~> literal ^^ { s => ForwardOperator(Uri.parse(s, prefixes)) }
  private def backwardOperator = "\\" ~> literal ^^ { s => BackwardOperator(Uri.parse(s, prefixes)) }
  private def filterOperator= "[" ~> (langFilter | propFilter) <~ "]"
  private def langFilter = "@lang" ~> compOperator ~ literal ^^ { case op ~ lang => LanguageFilter(op, lang) }
  private def propFilter = literal ~ compOperator ~ literal ^^ { case prop ~ op ~ value => PropertyFilter(prop, op, value) }

  private def literal = """<[^>]+>|[^\\/\[\] ]+""".r
  private def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}
