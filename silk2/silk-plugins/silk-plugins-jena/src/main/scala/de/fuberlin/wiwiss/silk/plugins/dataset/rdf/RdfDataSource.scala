package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import java.io.StringReader

import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

/**
 * A DataSource where all entities are given directly in the configuration.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-Triples", "Turtle"
 */
@Plugin(id = "rdf", label = "RDF", description = "A DataSource where all entities are given directly in the configuration.")
case class RdfDataSource(input: String, format: String) extends DataSource {

  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(input), null, format)

  private lazy val endpoint = new JenaModelEndpoint(model)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(endpoint, limit)
  }

}
