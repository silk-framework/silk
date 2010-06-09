package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.path._

/**
 * Builds SPARQL expressions.
 */
class SparqlBuilder(prefixes : Map[String, String], subjectVar : String, graphUri : Option[String])
{
    private var vars = new Vars

    private var restrictions = ""

    private var sparqlPatterns = ""

    def addRestriction(restriction : String) : Unit =
    {
        restrictions += (restriction + " .\n")
    }

    def addPath(path : Path) : Unit =
    {
        val pattern = build("?" + subjectVar, path.operators)
        sparqlPatterns += "OPTIONAL {\n" + pattern.replace(vars.curTempVar, vars.newValueVar(path)) + "}\n"
    }

    def build : String =
    {
        var sparql = ""
        sparql += prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString
        sparql += "SELECT DISTINCT ?" + subjectVar + " " + vars.valueVars.mkString(" ") + "\n"
        for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"
        sparql += "WHERE {\n"
        sparql += restrictions
        sparql += sparqlPatterns
        sparql += "}"

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
    private val ValueVarPrefix = "?v"

    private val TempVarPrefix = "?t"

    private val FilterVarPrefix = "?f"
}
