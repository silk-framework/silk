package org.silkframework.workspace.project

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion for workspace projects.
  */
case class ProjectReferenceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                                      workspace: WorkspaceReadTrait)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    filterResults(searchQuery, results(workspace))
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    results(workspace).find(_.value == value).flatMap(_.label)
  }

  private def results(workspace: WorkspaceReadTrait)(implicit userContext: UserContext): Seq[AutoCompletionResult] = {
    workspace.projects.map(_.config) map { projectConfig=>
      val label = projectConfig.metaData.label
      val value = projectConfig.id.toString
      AutoCompletionResult(value, Some(label))
    }
  }
}
