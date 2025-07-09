package org.silkframework.plugins.dataset.rdf.access

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.RepeatedIterator
import org.silkframework.runtime.plugin.PluginContext
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
                       (implicit context: PluginContext): EntityHolder = {
    val sourceIterator = sparqlSources.iterator
    val allEntities = new RepeatedIterator[Entity](() => sourceIterator.nextOption().map(_.retrieve(entitySchema, limit).entities))
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
                            (implicit context: PluginContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val sourceIterator = sparqlSources.iterator
      val allEntities = new RepeatedIterator[Entity](() => sourceIterator.nextOption().map(_.retrieveByUri(entitySchema, entities).entities))
      GenericEntityTable(allEntities, entitySchema, underlyingTask)
    }
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = Iterable.empty

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = IndexedSeq.empty
}
