package org.silkframework.plugins.dataset.rdf

import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.sparql.EntityRetriever
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Uri

@Plugin(id = "linkedData", label = "Linked Data", description = "TODO")
case class LinkedDataSource() extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit Limits the maximum number of retrieved entities
    *
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    throw new UnsupportedOperationException("Retrieving all entities not supported")
  }

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities The URIs of the entities to be retrieved.
    *
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    logger.log(Level.FINE, "Retrieving data from Linked Data.")

    val model = ModelFactory.createDefaultModel
    for (uri <- entities) {
      model.read(uri.uri)
    }

    val endpoint = new JenaModelEndpoint(model)

    val entityRetriever = EntityRetriever(endpoint)

    entityRetriever.retrieve(entitySchema, entities, None).toSeq
  }
}
