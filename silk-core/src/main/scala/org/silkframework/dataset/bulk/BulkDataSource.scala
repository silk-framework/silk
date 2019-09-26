package org.silkframework.dataset.bulk

import org.silkframework.config.Task
import org.silkframework.dataset._
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.ExecutionException
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

import scala.util.control.NonFatal

/**
  * A data source that merges the results of multiple data sources.
  *
  * @param bulkContainerName The name of the container, e.g. ZIP file name, directory name etc.
  * @param sources The underlying data sources whose results shall be merged and their resource names.
  * @param mergeSchemata If true, the types and paths of the underlying data sources are merged.
  *                      If false, the types and paths of the first data source are used.
  */
class BulkDataSource(bulkContainerName: String,
                     sources: Seq[DataSourceWithName],
                     mergeSchemata: Boolean) extends DataSource with PeakDataSource {
  require(sources.nonEmpty, "Tried to create a bulk data source with an empty list of sources.")

  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    if(mergeSchemata) {
      sourcesWithErrorHandler(_.retrieveTypes(limit)).flatten.distinct.toIndexedSeq
    } else {
      handleSourceError(sources.head)(_.retrieveTypes(limit))
    }
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    if(mergeSchemata) {
      sourcesWithErrorHandler(_.retrievePaths(typeUri, depth, limit)).flatten.distinct.toIndexedSeq
    } else {
      handleSourceError(sources.head)(_.retrievePaths(typeUri, depth, limit))
    }
  }

  private def sourcesWithErrorHandler[T](dataSourceFn: DataSource => T): Seq[T] = {
    for (source <- sources) yield {
      handleSourceError(source)(dataSourceFn)
    }
  }

  private def handleSourceError[T](source: DataSourceWithName)(dataSourceFn: DataSource => T): T = {
    try {
      dataSourceFn(source.source)
    } catch {
      case NonFatal(ex) =>
        throw new ExecutionException(s"Encountered error reading resource '${source.resourceName}' from inside container resource '$bulkContainerName'." +
            s" Error message: ${ex.getMessage}", Some(ex), abortExecution = true)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        sourcesWithErrorHandler { source =>
          for (entity <- source.retrieve(entitySchema, limit)) {
            f(entity)
          }
        }
      }
    }
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        sourcesWithErrorHandler { source =>
          for(entity <- source.retrieveByUri(entitySchema, entities)) {
            f(entity)
          }
        }
      }
    }
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = sources.head.source.underlyingTask
}
