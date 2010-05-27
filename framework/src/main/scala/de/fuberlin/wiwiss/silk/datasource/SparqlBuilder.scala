package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.path._

class SparqlBuilder
{
    private val subjects = Map("a" -> "dbpedia:Berlin", "b" -> "yago:Berlin")

    private var sparqlPatterns = ""

    def addPath(path : Path) : Unit =
    {
        sparqlPatterns = build("?" + path.variable, path.operators)
    }

    private def build(subjectVar : String, operators : List[PathOperator]) : String =
    {
        if(operators.isEmpty) return ""

        val operatorSparql = operators.head match
        {
            case ForwardOperator(property) => subjectVar + " " + property + " " + Vars.newVar + " .\n"
            case BackwardOperator(property) => Vars.newVar + " " + property + " " + subjectVar + " .\n"
            case LanguageFilter(op, lang) => "FILTER(lang(" + subjectVar + ") " + op + " " + lang + ") . \n"
            case PropertyFilter(property, op, value) => subjectVar + " " + property + " " + Vars.newFilterVar + " .\n" +
                                                        "FILTER(" + Vars.curFilterVar + " " + op + " " + value + ") . \n"
        }

        if(!operators.tail.isEmpty)
        {
            return operatorSparql + build(Vars.curVar, operators.tail)
        }
        else
        {
            return operatorSparql
        }
    }

    private object Vars
    {
        private val VarPrefix = "?v"

        private val FilterVarPrefix = "?f"

        private var varIndex = 0

        private var filterVarIndex = 0

        def newVar : String = { varIndex += 1; VarPrefix + varIndex }

        def curVar : String = VarPrefix + varIndex

        def newFilterVar : String = { filterVarIndex += 1; FilterVarPrefix + varIndex }

        def curFilterVar : String = FilterVarPrefix + filterVarIndex
    }
}
