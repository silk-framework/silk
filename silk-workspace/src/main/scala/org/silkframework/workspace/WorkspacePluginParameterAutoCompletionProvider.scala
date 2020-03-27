package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginParameterAutoCompletionProvider

/**
  * Adds some convenience methods for working with the workspace.
  */
trait WorkspacePluginParameterAutoCompletionProvider extends PluginParameterAutoCompletionProvider {
  /** Fetches the workspace. */
  protected def workspace(implicit userContext: UserContext): Workspace = WorkspaceFactory().workspace

  /** Fetches a project. */
  protected def project(projectId: String)(implicit userContext: UserContext): Project = workspace.project(projectId)
}
