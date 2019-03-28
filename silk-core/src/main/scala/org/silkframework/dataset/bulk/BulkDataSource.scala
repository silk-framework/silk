package org.silkframework.dataset.bulk

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

import scala.collection.mutable

class BulkDataSource(name: String, sources: Seq[DataSource with TypedPathRetrieveDataSource]) extends DataSource with TypedPathRetrieveDataSource {
  require(sources.nonEmpty, "Tried to create a bulk data source with an empty list of sources.")

  /**
    * Retrieves known types in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return any or all types.
    * The default implementation returns an empty traversable.
    *
    * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
    *
    */
  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]
    for (source <- sources) {
      val subResourceTypes: Traversable[(String, Double)] = source.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }

  /**
    * Retrieves the most frequent paths in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
    * The default implementation returns an empty traversable.
    *
    * @param typeUri The entity type for which paths shall be retrieved
    * @param depth   Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned. Since
    *                this value can be set to Int.MaxValue, the source has to make sure that it returns a result that
    *                can still be handled, e.g. it is Ok for XML and JSON to return all paths, for GRAPH data models this
    *                would be infeasible.
    * @param limit   Restricts the number of paths to be retrieved. If not given, all found paths are returned.
    * @return A Sequence of the found paths sorted by their frequency (most frequent first).
    */
  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext): IndexedSeq[Path] = {
    sources.head.retrievePaths(typeUri, depth, limit)
  }

  /**
    * Retrieves typed paths. The value type of the path denotes what type this path has in the corresponding data source.
    * The [[org.silkframework.entity.UriValueType]] has a special meaning for non-RDF data sources, in that it specifies
    * non-literal values, e.g. a XML element with nested elements, a JSON object or array of objects etc.
    *
    * @param typeUri The type URI. For non-RDF data types this is not a URI, e.g. XML or JSON this may express the path from the root.
    * @param depth   The maximum depths of the returned paths. This is only a limit, but not a guarantee that all paths
    *                of this length are actually returned.
    * @param limit   The maximum number of typed paths returned. None stands for unlimited.
    */
  override def retrieveTypedPath(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                       (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    sources.head.retrieveTypedPath(typeUri, depth, limit)
  }

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        for(source <- sources; entity <- source.retrieve(entitySchema, limit)) {
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
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        for(source <- sources; entity <- source.retrieveByUri(entitySchema, entities)) {
          f(entity)
        }
      }
    }
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(name), DatasetSpec(EmptyDataset))
}
