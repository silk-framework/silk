package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.StringReader
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever
import de.fuberlin.wiwiss.silk.instance.{Path, SparqlRestriction, InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.util.sparql.SparqlAggregatePathsCollector

/**
 * A DataSource where all instances are given directly in the configuration.
 *
 * Parameters:
 * - '''input''': The input data
 * - '''format''': The format of the input data. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@StrategyAnnotation(id = "rdf", label = "RDF", description = "A DataSource where all instances are given directly in the configuration.")
class RdfDataSource(input: String, format: String) extends DataSource {
  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(input), null, format)

  private lazy val endpoint = new JenaSparqlEndpoint(model)

  override def retrieve(instanceSpec: InstanceSpecification, instances: Seq[String]): Traversable[Instance] = {
    InstanceRetriever(endpoint).retrieve(instanceSpec, instances)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }
}
