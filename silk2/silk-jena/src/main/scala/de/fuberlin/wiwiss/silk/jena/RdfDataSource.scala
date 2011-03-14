package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.StringReader
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever

/**
 * A DataSource where all instances are given directly in the configuration.
 *
 * Parameters:
 * - '''input''': The input data
 * - '''format''': The format of the input data. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@StrategyAnnotation(id = "rdf", label = "RDF", description = "A DataSource where all instances are given directly in the configuration.")
class RdfDataSource(input : String, format : String) extends DataSource
{
  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) : Traversable[Instance] =
  {
    val reader = new StringReader(input)

    val model = ModelFactory.createDefaultModel
    model.read(reader, null, format)

    val endpoint = new JenaSparqlEndpoint(model)

    val instanceRetriever = InstanceRetriever(endpoint)

    instanceRetriever.retrieve(instanceSpec, instances)
  }
}
