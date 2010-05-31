package de.fuberlin.wiwiss.silk.datasource

import java.net.{URL, URLEncoder}
import xml.{Elem, XML}
import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.Configuration

class SparqlDataSource(val params : Map[String, String]) extends DataSource
{
    require(params.contains("endpointURI"), "Parameter 'endpointURI' is required")

    override def retrieve(config : Configuration, instance : InstanceSpecification) = new Traversable[Instance]
    {
        override def foreach[U](f : Instance => U) : Unit =
        {
            retrieveInstances(config, instance, f)
        }
    }

    private def retrieveInstances[U](config : Configuration, instance : InstanceSpecification, callback : Instance => U)
    {
        val builder = new SparqlBuilder(config.prefixes)
        builder.addRestriction(instance.restrictions)
        for(path <- instance.paths) builder.addPath(path)

        val sparql = builder.build
        println(sparql)

        val xml = query(sparql)

        //Remember current subject
        var curSubject : String = null

        //Collect values of the current subject
        val values = collection.mutable.HashMap[Int, Set[String]]()

        for(result <- xml \ "results" \ "result")
        {
            val bindings = result \ "binding"

            //Find binding for subject variable
            for(subjectBinding <- bindings.find(binding => (binding \ "@name").text == "s"))
            {
                val subject = subjectBinding.head.text

                //Check if we are still reading values for the current subject
                if(subject != curSubject)
                {
                    if(curSubject != null)
                    {
                        callback(new Instance(curSubject, values.toMap))
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
                    val varValue = binding.head.text

                    val oldVarValues = values.get(id).getOrElse(Set())

                    values(id) = oldVarValues + varValue
                }
            }
        }
    }

    def queryInstances(instance : InstanceSpecification)
    {
        //TODO set dataset: params("graph")

        var sparql = "SELECT DISTINCT ?s\n"
        sparql += "WHERE {\n"
        sparql += "?s ?s_p ?s_o\n"
        sparql += "} LIMIT 100"

        //TODO restrictions
        //TODO limit & offset

        println(query(sparql))
    }

    def query(query : String) : Elem =
    {
        XML.load(new URL(params("endpointURI") + "?format=application/rdf+xml&query=" + URLEncoder.encode(query, "UTF-8")))
    }
}
