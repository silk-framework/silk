package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.StringReader
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.sparql.EntityRetriever
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, EntityDescription, Entity}
import de.fuberlin.wiwiss.silk.util.sparql.SparqlAggregatePathsCollector

/**
 * A DataSource where all entities are given directly in the configuration.
 *
 * Parameters:
 * - '''input''': The input data
 * - '''format''': The format of the input data. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@Plugin(id = "rdf", label = "RDF", description = "A DataSource where all entities are given directly in the configuration.")
class RdfDataSource(input: String, format: String) extends DataSource {
  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(input), null, format)

  private lazy val endpoint = new JenaSparqlEndpoint(model)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }
}
