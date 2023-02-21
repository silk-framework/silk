package org.silkframework.workspace.project.task

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait
import org.silkframework.workspace.activity.workflow.Workflow

/**
  * Auto-completion for workflow tasks.
  */
case class WorkflowTaskReferenceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    val taskProject = getProject(dependOnParameterValues)
    val allWorkflows = workspace.project(taskProject).tasks[Workflow].map(w => AutoCompletionResult(w.id, w.metaData.label))
    filterResults(searchQuery, allWorkflows)
  }

  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    implicit val userContext: UserContext = context.user
    val taskProject = getProject(dependOnParameterValues)
    workspace.project(taskProject).taskOption[Workflow](value).flatMap(_.metaData.label)
  }
}
