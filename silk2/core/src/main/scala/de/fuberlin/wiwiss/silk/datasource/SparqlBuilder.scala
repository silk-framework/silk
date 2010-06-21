package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.path._
import de.fuberlin.wiwiss.silk.Instance
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.util.SparqlEndpoint

/**
 * Builds SPARQL expressions.
 */
class SparqlBuilder(endpoint : SparqlEndpoint, pageSize : Int, graphUri : Option[String] = None)
{
    private val ValueVarPrefix = "?v"

    private val TempVarPrefix = "?t"

    private val FilterVarPrefix = "?f"

    def execute(instanceSpec : InstanceSpecification, subject : Option[String] = None, prefixes : Map[String, String] = Map.empty) : Traversable[Instance] =
    {
        val vars = new Vars

        val subjectVar = subject.map("<" + _ + ">").getOrElse("?" + instanceSpec.variable)

        val pathPatterns = instanceSpec.paths.map(path => buildPath(subjectVar, path.operators, vars).replace(vars.curTempVar, vars.newValueVar(path)))
                                             .mkString("OPTIONAL {\n", "", "}\n")


        //Prefixes
        var sparql = prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

        //Select
        sparql += "SELECT DISTINCT "
        sparql += (if(subject.isEmpty) "?" + instanceSpec.variable + " " else "")
        sparql += vars.valueVars.mkString(" ") + "\n"

        //Graph
        for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"

        //Body
        sparql += "WHERE {\n"
        sparql += instanceSpec.restrictions + "\n"
        sparql += pathPatterns
        sparql += "}"

        new InstanceTraversable(sparql, instanceSpec, subject)
    }

    private def buildPath(subject : String, operators : List[PathOperator], vars : Vars) : String =
    {
        if(operators.isEmpty) return ""

        val operatorSparql = operators.head match
        {
            case ForwardOperator(property) => subject + " " + property + " " + vars.newTempVar + " .\n"
            case BackwardOperator(property) => vars.newTempVar + " " + property + " " + subject + " .\n"
            case LanguageFilter(op, lang) => "FILTER(lang(" + subject + ") " + op + " " + lang + ") . \n"
            case PropertyFilter(property, op, value) => subject + " " + property + " " + vars.newFilterVar + " .\n" +
                                                        "FILTER(" + vars.curFilterVar + " " + op + " " + value + ") . \n"
        }

        if(!operators.tail.isEmpty)
        {
            return operatorSparql + buildPath(vars.curTempVar, operators.tail, vars)
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

        def newTempVar : String = { tempVarIndex += 1; TempVarPrefix + tempVarIndex }

        def curTempVar : String = TempVarPrefix + tempVarIndex

        def newFilterVar : String = { filterVarIndex += 1; FilterVarPrefix + tempVarIndex }

        def curFilterVar : String = FilterVarPrefix + filterVarIndex

        def newValueVar(path : Path) : String =
        {
            val valueVar = ValueVarPrefix + path.id
            valueVars += valueVar
            valueVar
        }
    }

    private class InstanceTraversable(sparql : String, instanceSpec : InstanceSpecification, subject : Option[String]) extends Traversable[Instance]
    {
        override def foreach[U](f : Instance => U) : Unit =
        {
            //Create SPARQL query
            val reader = new SparqlReader(instanceSpec, subject, f)

            //Issue queries
            for(offset <- 0 until Integer.MAX_VALUE by pageSize)
            {
                val xml = endpoint.query(sparql + " OFFSET " + offset + " LIMIT " + pageSize)

                val results = xml \ "results" \ "result"

                if(reader.read(results)) return
            }
        }
    }

    private class SparqlReader[U](instanceSpec : InstanceSpecification, subject : Option[String], f : Instance => U)
    {
        //Remember current subject
        var curSubject : String = subject.getOrElse(null)

        //Collect values of the current subject
        val values = collection.mutable.HashMap[Int, Set[String]]()

        /**
         * Reads the Query Results
         *
         * @param results The query results in the SPARQL Query Results Format
         * @returns True, if this was the last query result 
         */
        def read(results : NodeSeq) : Boolean =
        {
            for(result <- results)
            {
                val bindings = result \ "binding"

                //If the subject is unknown, find binding for subject variable
                if(subject.isEmpty)
                {
                    for(subjectBinding <- bindings.find(binding => (binding \ "@name").text.trim == instanceSpec.variable))
                    {
                        val subject = subjectBinding.head.text.trim

                        //Check if we are still reading values for the current subject
                        if(subject != curSubject)
                        {
                            if(curSubject != null)
                            {
                                f(new Instance(instanceSpec.variable, curSubject, values.toMap))
                            }

                            curSubject = subject
                            values.clear()
                        }
                    }
                }

                //Find bindings for values for the current subject
                for(binding <- bindings;
                    varNode <- binding \ "@name";
                    varName = varNode.text if varName.startsWith("v"))
                {
                    val id = varName.tail.toInt
                    val varValue = binding.head.text.trim

                    val oldVarValues = values.get(id).getOrElse(Set())

                    values(id) = oldVarValues + varValue
                }
            }

            if(results.size < pageSize)
            {
                f(new Instance(instanceSpec.variable, curSubject, values.toMap))
                true
            }
            else
            {
                false
            }
        }
    }
}
