package org.silkframework.plugins.dataset.rdf.access

import org.silkframework.config.Task
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * A helper data source to combine several SPARQL sources in order to retrieve entities from them.
  * The sources are not merged, but instead are queries one by one.
  */
case class CombinedSparqlSource(underlyingTask: Task[DatasetSpec[Dataset]], sparqlSources: SparqlSource*) extends DataSource {
  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext): EntityHolder = {
    val allEntities =
      new Traversable[Entity] {
        override def foreach[U](f: Entity => U): Unit = {
          for (sparqlSource <- sparqlSources;
               entity <- sparqlSource.retrieve(entitySchema, limit).entities) {
            f(entity)
          }
        }
      }
    GenericEntityTable(allEntities, entitySchema, underlyingTask)
  }

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val results = for (sparqlSource <- sparqlSources) yield {
        sparqlSource.retrieveByUri(entitySchema, entities).entities
      }
      GenericEntityTable(results.flatten, entitySchema, underlyingTask)
    }
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = Traversable.empty

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = IndexedSeq.empty
}
