package de.fuberlin.wiwiss.silk.datasource

import scala.util.parsing.combinator._

case class Path(variable : String, operators : List[Operator])

sealed abstract class Operator
case class ForwardOperator(property : String) extends Operator
case class BackwardOperator(property : String) extends Operator
case class LanguageFilter(operator : String, language : String) extends Operator
case class PropertyFilter(property : String, operator : String, value : String) extends Operator

class PathParser extends RegexParsers
{
    def path = variable ~ rep(forwardOperator | backwardOperator | filterOperator) ^^ { case variable ~ operators => Path(variable, operators) }

    def variable = "?" ~> literal
    def forwardOperator = "/" ~> literal ^^ { s => ForwardOperator(s) }
    def backwardOperator = "\\" ~> literal ^^ { s => BackwardOperator(s) }
    def filterOperator= "[" ~> (langFilter | propFilter) <~ "]"
    def langFilter = "@lang" ~> compOperator ~ literal ^^ { case op ~ lang => LanguageFilter(op, lang) }
    def propFilter = literal ~ compOperator ~ literal ^^ { case prop ~ op ~ value => PropertyFilter(prop, op, value) }

    def literal = """[^\\/\[\] ]+""".r
    def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}

object SparqlBuilder
{
    private val subjects = Map("a" -> "dbpedia:Berlin", "b" -> "yago:Berlin")

    def build(path : Path) : String =
    {
        build("?" + path.variable, path.operators, "?" + path.variable + "Res")
    }

    private def build(subjectVar : String, operators : List[Operator], objectVar : String) : String =
    {
        if(operators.isEmpty) return ""

        val variable = if(operators.tail.isEmpty) objectVar else objectVar + operators.size

        val operatorSparql = operators.head match
        {
            case ForwardOperator(property) => subjectVar + " " + property + " " + variable + " .\n"
            case BackwardOperator(property) => variable + " " + property + " " + subjectVar + " .\n"
            case LanguageFilter(op, lang) => "FILTER(lang(" + subjectVar + ") " + op + " " + lang + ") . \n"
            case PropertyFilter(property, op, value) => subjectVar + " " + property + " " + variable + " .\n" +
                                                        "FILTER(" + variable + " " + op + " " + value + ") . \n"
        }

        if(!operators.tail.isEmpty)
        {
            return operatorSparql + build(variable, operators.tail, objectVar)
        }
        else
        {
            return operatorSparql
        }
    }
}
