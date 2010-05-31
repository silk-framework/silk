package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.path._

/**
 * Builds SPARQL expressions.
 */
class SparqlBuilder(prefixes : Map[String, String])
{
    private val subjects = Map("a" -> "dbpedia:Berlin", "b" -> "yago:Berlin")

    private var vars = new Vars

    private var restrictions = ""

    private var sparqlPatterns = ""

    def addRestriction(restriction : String) : Unit =
    {
        restrictions += (restriction + " .\n")
    }

    def addPath(path : Path) : Unit =
    {
        val pattern = build(SparqlBuilder.SubjectVar, path.operators)
        sparqlPatterns += pattern.replace(vars.curTempVar, vars.newValueVar(path))
    }

    def build : String =
    {
        //TODO prefixes
        var sparql = ""
        sparql += prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString
        sparql += "SELECT DISTINCT " + SparqlBuilder.SubjectVar + " " + vars.valueVars.mkString(" ") + "\n"
        //TODO add graphs
        sparql += "WHERE {\n"
        sparql += restrictions
        sparql += sparqlPatterns
        sparql += "} LIMIT 100"
        //TODO limit offset

        sparql
    }

    private def build(subjectVar : String, operators : List[PathOperator]) : String =
    {
        if(operators.isEmpty) return ""

        val operatorSparql = operators.head match
        {
            case ForwardOperator(property) => subjectVar + " " + property + " " + vars.newTempVar + " .\n"
            case BackwardOperator(property) => vars.newTempVar + " " + property + " " + subjectVar + " .\n"
            case LanguageFilter(op, lang) => "FILTER(lang(" + subjectVar + ") " + op + " " + lang + ") . \n"
            case PropertyFilter(property, op, value) => subjectVar + " " + property + " " + vars.newFilterVar + " .\n" +
                                                        "FILTER(" + vars.curFilterVar + " " + op + " " + value + ") . \n"
        }

        if(!operators.tail.isEmpty)
        {
            return operatorSparql + build(vars.curTempVar, operators.tail)
        }
        else
        {
            return operatorSparql
        }
    }

    private class Vars
    {
        private var tempVarIndex = 0

        private var filterVarIndex = 0

        var valueVars = Set[String]()

        def newTempVar : String = { tempVarIndex += 1; SparqlBuilder.TempVarPrefix + tempVarIndex }

        def curTempVar : String = SparqlBuilder.TempVarPrefix + tempVarIndex

        def newFilterVar : String = { filterVarIndex += 1; SparqlBuilder.FilterVarPrefix + tempVarIndex }

        def curFilterVar : String = SparqlBuilder.FilterVarPrefix + filterVarIndex

        def newValueVar(path : Path) : String =
        {
            val valueVar = SparqlBuilder.ValueVarPrefix + path.id
            valueVars += valueVar
            valueVar
        }
    }
}

private object SparqlBuilder
{
    private val SubjectVar = "?s"

    private val ValueVarPrefix = "?v"

    private val TempVarPrefix = "?t"

    private val FilterVarPrefix = "?f"
}
