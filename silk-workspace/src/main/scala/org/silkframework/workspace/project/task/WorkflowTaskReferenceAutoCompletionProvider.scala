package org.silkframework.workspace.project.task

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AutoCompletionResult
import org.silkframework.workspace.WorkspacePluginParameterAutoCompletionProvider
import org.silkframework.workspace.activity.workflow.Workflow

/**
  * Auto-completion for workflow tasks.
  */
case class WorkflowTaskReferenceAutoCompletionProvider() extends WorkspacePluginParameterAutoCompletionProvider {
  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String])
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val taskProject = dependOnParameterValues.headOption.getOrElse(projectId)
    val allWorkflows = project(taskProject).tasks[Workflow].map(w => AutoCompletionResult(w.id, Some(w.metaData.label)))
    filterResults(searchQuery, allWorkflows)
  }

  /**
    * @param dependOnParameterValues If at least one value is specified, this will use the first parameter as project ID
    *                                instead of the projectId parameter.
    */
  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String])
                           (implicit userContext: UserContext): Option[String] = {
    val taskProject = dependOnParameterValues.headOption.getOrElse(projectId)
    project(taskProject).taskOption[Workflow](value).map(_.metaData.label)
  }
}
