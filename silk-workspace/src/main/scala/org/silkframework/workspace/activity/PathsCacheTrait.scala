package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, TypedPathRetrieveDataSource}
import org.silkframework.entity.TypedPath
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.transform.CachedEntitySchemata

/**
  * Defines methods useful to all paths caches.
  */
trait PathsCacheTrait {
  def retrievePathsOfInput(taskId: Identifier,
                           dataSelection: Option[DatasetSelection],
                           task: ProjectTask[_],
                           context: ActivityContext[CachedEntitySchemata])
                          (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    task.project.anyTask(taskId).data match {
      case dataset: DatasetSpec[Dataset] =>
        context.status.update("Retrieving frequent paths", 0.0)
        dataSelection match {
          case Some(selection) =>
            dataset.plugin.source match {
              case typedDataSource: TypedPathRetrieveDataSource =>
                typedDataSource.retrieveTypedPath(selection.typeUri)
              case ds: DataSource =>
                ds.retrievePaths(selection.typeUri, Int.MaxValue).map(_.asStringTypedPath)
            }
          case None =>
            IndexedSeq()
        }
      case task: TaskSpec =>
        task.outputSchemaOpt match {
          case Some(schema) =>
            schema.typedPaths
          case None =>
            IndexedSeq()
        }
    }

  }
}
