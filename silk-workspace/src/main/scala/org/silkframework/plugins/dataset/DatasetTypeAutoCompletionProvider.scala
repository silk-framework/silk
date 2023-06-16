package org.silkframework.plugins.dataset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.validation.ValidationException
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
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    val projectId = context.projectId.getOrElse(throw new ValidationException("Project not provided"))
    dependOnParameterValues.headOption.
        flatMap(datasetId => workspace.project(projectId).anyTaskOption(datasetId.strValue)).
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
  override def valueToLabel(value: String,
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    None
  }
}
