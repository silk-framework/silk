package org.silkframework.dataset.bulk

import org.silkframework.config.Task
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * A data source that merges the results of multiple data sources.
  *
  * @param sources The underlying data sources whose results shall be merged.
  * @param mergeSchemata If true, the types and paths of the underlying data sources are merged.
  *                      If false, the types and paths of the first data source are used.
  */
class BulkDataSource(sources: Seq[DataSource with TypedPathRetrieveDataSource],
                     mergeSchemata: Boolean) extends DataSource with TypedPathRetrieveDataSource with PeakDataSource {
  require(sources.nonEmpty, "Tried to create a bulk data source with an empty list of sources.")

  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    if(mergeSchemata) {
      sources.flatMap(_.retrieveTypes(limit)).distinct.toIndexedSeq
    } else {
      sources.head.retrieveTypes(limit)
    }
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext): IndexedSeq[Path] = {
    if(mergeSchemata) {
      sources.flatMap(_.retrievePaths(typeUri, depth, limit)).distinct.toIndexedSeq
    } else {
      sources.head.retrievePaths(typeUri, depth, limit)
    }
  }

  override def retrieveTypedPath(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                       (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    if(mergeSchemata) {
      sources.flatMap(_.retrieveTypedPath(typeUri, depth, limit)).distinct.toIndexedSeq
    } else {
      sources.head.retrieveTypedPath(typeUri, depth, limit)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        for(source <- sources; entity <- source.retrieve(entitySchema, limit)) {
          f(entity)
        }
      }
    }
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        for(source <- sources; entity <- source.retrieveByUri(entitySchema, entities)) {
          f(entity)
        }
      }
    }
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = sources.head.underlyingTask
}
