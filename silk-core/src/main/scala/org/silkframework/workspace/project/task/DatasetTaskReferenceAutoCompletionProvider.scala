package org.silkframework.workspace.project.task

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion for dataset tasks.
  */
case class DatasetTaskReferenceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    val allDatasets = workspace.project(getProject(dependOnParameterValues)).tasks[GenericDatasetSpec].map(spec => AutoCompletionResult(spec.id, spec.metaData.label))
    filterResults(searchQuery, allDatasets)
  }

  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    implicit val userContext: UserContext = context.user
    workspace.project(getProject(dependOnParameterValues)).taskOption[GenericDatasetSpec](value).flatMap(_.metaData.label)
  }
}
