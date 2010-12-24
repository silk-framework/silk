package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever
import java.io.FileInputStream

/**
 * DataSource which retrieves all instances from an RDF file.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
class FileDataSource(val params : Map[String, String]) extends DataSource
{
  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) =
  {
    val model = ModelFactory.createDefaultModel
    model.read(new FileInputStream(readRequiredParam("file")), null, readRequiredParam("format"))

    val endpoint = new JenaSparqlEndpoint(model, instanceSpec.prefixes)

    val instanceRetriever = new InstanceRetriever(endpoint)

    instanceRetriever.retrieve(instanceSpec, instances)
  }
}
