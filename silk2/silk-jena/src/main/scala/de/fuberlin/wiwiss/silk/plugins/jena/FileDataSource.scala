package de.fuberlin.wiwiss.silk.plugins.jena

import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.io.FileInputStream
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.sparql.{EntityRetriever, SparqlAggregatePathsCollector}

/**
 * DataSource which retrieves all instances from an RDF file.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@Plugin(id = "file", label = "RDF dump", description = "DataSource which retrieves all entities from an RDF file.")
class FileDataSource(file: String, format: String) extends DataSource {
  private lazy val model = ModelFactory.createDefaultModel
  model.read(new FileInputStream(file), null, format)

  private lazy val endpoint = new JenaSparqlEndpoint(model)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }
}
