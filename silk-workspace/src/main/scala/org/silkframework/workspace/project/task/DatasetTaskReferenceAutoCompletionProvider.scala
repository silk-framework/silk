package org.silkframework.workspace.project.task

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AutoCompletionResult
import org.silkframework.workspace.WorkspacePluginParameterAutoCompletionProvider

/**
  * Auto-completion for dataset tasks.
  */
case class DatasetTaskReferenceAutoCompletionProvider() extends WorkspacePluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String])
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val taskProject = dependOnParameterValues.headOption.getOrElse(projectId)
    val allDatasets = project(taskProject).tasks[GenericDatasetSpec].map(spec => AutoCompletionResult(spec.id, Some(spec.metaData.label)))
    filterResults(searchQuery, allDatasets)
  }

  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String])
                           (implicit userContext: UserContext): Option[String] = {
    val taskProject = dependOnParameterValues.headOption.getOrElse(projectId)
    project(taskProject).taskOption[GenericDatasetSpec](value).map(_.metaData.label)
  }
}

