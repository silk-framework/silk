package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever

/**
 * DataSource which retrieves all instances from an RDF file.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
class FileDataSource(val params : Map[String, String]) extends DataSource
{
    private val instanceRetriever =
    {
        val model = ModelFactory.createDefaultModel
        model.read(readRequiredParam("file"), null, readRequiredParam("format"))

        val endpoint = new JenaSparqlEndpoint(model)

        new InstanceRetriever(endpoint)
    }

    override def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String]) =
    {
        instanceRetriever.retrieveAll(instanceSpec, prefixes)
    }
}
