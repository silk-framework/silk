package org.silkframework.workspace.activity

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

/**
  * Defines methods useful to all paths caches.
  */
trait PathsCacheTrait {

  protected def maxDepth: Int = 1

  protected def maxPaths: Option[Int] = None

  protected def retrievePathsOfInput(inputTaskId: Identifier,
                                     dataSelection: Option[DatasetSelection],
                                     project: Project,
                                     context: ActivityContext[_])
                                    (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    project.anyTask(inputTaskId).data match {
      case dataset: DatasetSpec[Dataset] =>
        context.status.update("Retrieving frequent paths", 0.0)
        dataSelection match {
          case Some(selection) =>
            dataset.source.retrievePaths(selection.typeUri, Int.MaxValue)
          case None => IndexedSeq()
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
