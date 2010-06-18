package de.fuberlin.wiwiss.silk.datasource

import java.net.{URL, URLEncoder}
import de.fuberlin.wiwiss.silk.Instance
import xml.{NodeSeq, Elem, XML}
import de.fuberlin.wiwiss.silk.util.SparqlEndpoint

class SparqlDataSource(val params : Map[String, String]) extends DataSource
{
    private val endpoint = new SparqlEndpoint(
                                   uri = readRequiredParam("endpointURI"),
                                   pauseTime = readOptionalIntParam("pauseTime").getOrElse(0),
                                   retryCount = readOptionalIntParam("retryCount").getOrElse(3),
                                   retryPause = readOptionalIntParam("retryPause").getOrElse(1000)
                               )

    val pageSize = readOptionalIntParam("pageSize").getOrElse(1000)

    override def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String] = Map.empty) = new Traversable[Instance]
    {
        override def foreach[U](f : Instance => U) : Unit =
        {
            //Create SPARQL query
            val builder = new SparqlBuilder(prefixes, instanceSpec.variable, params.get("graph"), instanceSpec.restrictions)
            for(path <- instanceSpec.paths) builder.addPath(path)
            val sparql = builder.build

            val reader = new SparqlReader(instanceSpec, f)

            //Issue queries
            for(offset <- 0 until Integer.MAX_VALUE by pageSize)
            {
                val xml = endpoint.query(sparql + "OFFSET " + offset + " LIMIT " + pageSize)
                val results = xml \ "results" \ "result"

                if(results.isEmpty)
                {
                    return
                }
                else
                {
                    reader.read(results)
                }
            }
        }
    }

    class SparqlReader[U](instanceSpec : InstanceSpecification, f : Instance => U)
    {
        //Remember current subject
        var curSubject : String = null

        //Collect values of the current subject
        val values = collection.mutable.HashMap[Int, Set[String]]()

        def read(results : NodeSeq) : Unit =
        {
            for(result <- results)
            {
                val bindings = result \ "binding"

                //Find binding for subject variable
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
            }
        }
    }
}
