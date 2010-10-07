package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever
import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import java.io.StringReader

/**
 * A DataSource where all instances are given directly in the configuration.
 *
 * Parameters:
 * - '''input''': The input data
 * - '''format''': The format of the input data. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
class RdfDataSource(val params : Map[String, String]) extends DataSource
{
    private val instanceRetriever =
    {
        val reader = new StringReader(readRequiredParam("input"))

        val model = ModelFactory.createDefaultModel
        model.read(reader, null, readRequiredParam("format"))

        val endpoint = new JenaSparqlEndpoint(model)

        new InstanceRetriever(endpoint)
    }

    override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) : Traversable[Instance] =
    {
        instanceRetriever.retrieve(instanceSpec, instances)
    }
}