package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.util.Uri

/**
  * Created on 6/9/16.
  */
case class CombinedSparqlSource(sparqlSources: SparqlSource*) extends DataSource {
  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: (Entity) => U): Unit = {
        for (sparqlSource <- sparqlSources;
             entity <- sparqlSource.retrieve(entitySchema, limit)) {
          f(entity)
        }
      }
    }
  }

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    val results = for (sparqlSource <- sparqlSources) yield {
      sparqlSource.retrieveByUri(entitySchema, entities)
    }
    results.flatten
  }
}
