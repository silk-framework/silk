package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.path._
import de.fuberlin.wiwiss.silk.Instance
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlPathBuilder, Node, SparqlEndpoint}

/**
 * Builds SPARQL expressions.
 */
class SparqlBuilder(endpoint : SparqlEndpoint, pageSize : Int, graphUri : Option[String] = None)
{
    private val varPrefix = "v"

    def execute(instanceSpec : InstanceSpecification, subject : Option[String] = None, prefixes : Map[String, String] = Map.empty) : Traversable[Instance] =
    {
        val subjectVar = subject.map("<" + _ + ">").getOrElse("?" + instanceSpec.variable)

        val pathPatterns = SparqlPathBuilder(instanceSpec.paths, subjectVar, "?" + varPrefix)

        //Prefixes
        var sparql = prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

        //Select
        sparql += "SELECT DISTINCT "
        sparql += (if(subject.isEmpty) "?" + instanceSpec.variable + " " else "")
        sparql += instanceSpec.paths.map("?" + varPrefix + _.id).mkString(" ") + "\n"

        //Graph
        for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"

        //Body
        sparql += "WHERE {\n"
        sparql += instanceSpec.restrictions + "\n"
        sparql += pathPatterns
        sparql += "}"

        val sparqlResults = endpoint.query(sparql)

        new InstanceTraversable(sparqlResults, instanceSpec, subject)
    }

    /**
     * Wraps a Traversable of SPARQL results and retrieves instances from them.
     */
    private class InstanceTraversable(sparqlResults : Traversable[Map[String, Node]], instanceSpec : InstanceSpecification, subject : Option[String]) extends Traversable[Instance]
    {
        override def foreach[U](f : Instance => U) : Unit =
        {
            //Remember current subject
            var curSubject : String = subject.getOrElse(null)

            //Collect values of the current subject
            val values = collection.mutable.HashMap[Int, Set[String]]()

            for(result <- sparqlResults)
            {
                //If the subject is unknown, find binding for subject variable
                if(subject.isEmpty)
                {
                    //Check if we are still reading values for the current subject
                    val resultSubject = result.get(instanceSpec.variable).get
                    if(resultSubject != curSubject)
                    {
                        if(curSubject != null)
                        {
                            f(new Instance(instanceSpec.variable, curSubject, values.toMap))
                        }

                        curSubject = resultSubject.value
                        values.clear()
                    }
                }
                
                //Find results for values for the current subject
                for((variable, node) <- result if variable.startsWith(varPrefix))
                {
                    val id = variable.substring(varPrefix.length).toInt

                    val oldVarValues = values.get(id).getOrElse(Set())

                    values(id) = oldVarValues + node.value
                }
            }

            if(!values.isEmpty)
            {
                f(new Instance(instanceSpec.variable, curSubject, values.toMap))
            }
        }
    }
}
