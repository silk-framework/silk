package org.silkframework.dataset.bulk

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.execution.{EntityHolder, ExecutionException, MappedTraversable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

import scala.collection.mutable
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
                     sources: Traversable[DataSourceWithName],
                     mergeSchemata: Boolean) extends DataSource with PeakDataSource {

  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    if(mergeSchemata) {
      mergePaths[(String, Double), String](
        _.retrieveTypes(limit),
        indexFn = weightedPath => weightedPath._1 // Make distinct by path name only
      )
    } else {
      sources.headOption.map(handleSourceError(_)(_.retrieveTypes(limit))).getOrElse(Seq.empty)
    }
  }

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    if(mergeSchemata) {
      mergePaths[TypedPath, TypedPath](
        _.retrievePaths(typeUri, depth, limit),
        indexFn = a => a
      ).toIndexedSeq
    } else {
      sources.headOption.map(handleSourceError(_)(_.retrievePaths(typeUri, depth, limit))).getOrElse(IndexedSeq.empty)
    }
  }

  // Merge the paths with memory foot print max. the size of the number of paths in the result.
  // indexFn extracts the part of the element that should be distinguished by.
  private def mergePaths[T, U](dataSourcePathFn: DataSource => Traversable[T],
                               indexFn: T => U): Traversable[T] = {
    new Traversable[T] {
      override def foreach[V](f: T => V): Unit = {
        val entrySet = new mutable.HashSet[U]()
        sources foreach { source =>
          handleSourceError(source) { dataSource =>
            for (elem <- dataSourcePathFn(dataSource)) {
              // Only emit path once, do not distinguish the same path with different weight
              val valueToIndex = indexFn(elem)
              if (!entrySet.contains(valueToIndex)) {
                entrySet.add(valueToIndex)
                f(elem)
              }
            }
          }
        }
      }
    }
  }

  // Report errors from a data source that happen inside the given block with useful information
  private def handleSourceError[T](source: DataSourceWithName)(dataSourceFn: DataSource => T): T = {
    try {
      dataSourceFn(source.source)
    } catch {
      case NonFatal(ex) =>
        throw new ExecutionException(s"Encountered error reading resource '${source.resourceName}' from inside container resource '$bulkContainerName'." +
            s" Error message: ${ex.getMessage}", Some(ex), abortExecution = true)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): EntityHolder = {
    val entities =
      new Traversable[Entity] {
        override def foreach[U](emitEntity: Entity => U): Unit = {
          sources foreach { dataSource =>
            handleSourceError(dataSource) { source =>
              source.retrieve(entitySchema, limit).entities foreach emitEntity
            }
          }
        }
      }

    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): EntityHolder = {
    val retrievedEntities =
      new Traversable[Entity] {
        override def foreach[U](f: Entity => U): Unit = {
          sources foreach { dataSource =>
            handleSourceError(dataSource) { source =>
              for(entity <- source.retrieveByUri(entitySchema, entities).entities) {
                f(entity)
              }
            }
          }
        }
      }

    GenericEntityTable(retrievedEntities, entitySchema, underlyingTask)
  }

  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(bulkContainerName), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}
