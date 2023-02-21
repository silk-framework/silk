package org.silkframework.rule.task

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completes task references that are either of type Dataset or TransformSpec.
  */
case class DatasetOrTransformTaskAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    val taskProject = getProject(dependOnParameterValues)
    val allDatasets = workspace.project(taskProject).tasks[GenericDatasetSpec]
    val allTransformTasks = workspace.project(taskProject).tasks[TransformSpec]
    val all = (allDatasets ++ allTransformTasks).map(spec => AutoCompletionResult(spec.id, spec.metaData.label))
    filterResults(searchQuery, all)
  }

  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    implicit val userContext: UserContext = context.user
    workspace.project(getProject(dependOnParameterValues))
      .anyTaskOption(value)
      .flatMap(_.metaData.label)
      .filter(_.nonEmpty) // No empty string labels. This can still happen when loading old projects
  }
}