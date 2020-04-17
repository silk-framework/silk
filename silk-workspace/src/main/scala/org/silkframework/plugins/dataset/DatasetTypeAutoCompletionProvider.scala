package org.silkframework.plugins.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.{ProjectTask, WorkspaceReadTrait}

/**
  * Provides auto-completion for dataset types.
  */
@Plugin(
  id = "datasetTypeAutoCompletionProvider",
  label = "Dataset type auto-completion provider"
)
case class DatasetTypeAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues It assumes to get the dataset ID in first position.
    */
  override def autoComplete(searchQuery: String,
                            projectId: String,
                            dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    dependOnParameterValues.headOption.
        flatMap(datasetId => workspace.project(projectId).anyTaskOption(datasetId)).
        toSeq.flatMap {
          case task: ProjectTask[_] =>
            try {
              val activity = task.activity[TypesCache]
              filterStringResults(searchQuery, activity.value().types)
            } catch {
              case _: NoSuchElementException =>
                Seq()
            }
          case _ => None
        }
  }

  /**
    * There are no labels for types in the type cache.
    */
  override def valueToLabel(projectId: String,
                            value: String,
                            dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}
