package org.silkframework.plugins.dataset.rdf

import java.io.StringReader
import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlRestrictionBuilder, SparqlEntitySchema}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Uri

/**
 * A DataSource where all entities are given directly in the configuration.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-Triples", "Turtle"
 */
@Plugin(id = "rdf", label = "RDF", description = "A DataSource where all entities are given directly in the configuration.")
case class RdfDataSource(input: String, format: String) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(input), null, format)

  private lazy val endpoint = new JenaModelEndpoint(model)

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(endpoint, limit)
  }

  override def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    val restriction = SparqlRestriction.fromSparql("a", s"?a a <$t>")
    SparqlAggregatePathsCollector(endpoint, restriction, limit)
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from RDF.")
    // TODO limit is currently ignored
    EntityRetriever(endpoint).retrieve(SparqlEntitySchema.fromSchema(entitySchema), Seq.empty, limit)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Option[Entity]] = {
    val entitiesByUri = EntityRetriever(endpoint).retrieve(SparqlEntitySchema.fromSchema(entitySchema), entities, None).groupBy(_.uri)
    entities.map(e => entitiesByUri.get(e.uri).map(_.head))
  }

  ////////////////////////////////////////
  // TODO remove deprecated methods below
  ////////////////////////////////////////

  override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String]): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from RDF.")
    EntityRetriever(endpoint).retrieve(entityDesc, entities.map(Uri(_)), None)
  }

  override def retrieveSparqlPaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit).map(p => (p, 1.0))
  }



}
