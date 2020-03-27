package org.silkframework.workspace.project

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AutoCompletionResult
import org.silkframework.workspace.{WorkspaceFactory, WorkspacePluginParameterAutoCompletionProvider}

/**
  * Auto-completion for workspace projects.
  */
case class ProjectReferenceAutoCompletionProvider() extends WorkspacePluginParameterAutoCompletionProvider {
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String])
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    filterResults(searchQuery, results)
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String])
                           (implicit userContext: UserContext): Option[String] = {
    results.find(_.value == value).flatMap(_.label)
  }

  private def results(implicit userContext: UserContext): Seq[AutoCompletionResult] = {
    workspace.projects.map(_.config) map { projectConfig=>
      val label = projectConfig.metaData.label
      val value = projectConfig.id.toString
      AutoCompletionResult(value, Some(label))
    }
  }
}
