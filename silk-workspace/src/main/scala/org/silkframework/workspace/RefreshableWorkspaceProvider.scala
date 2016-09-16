package org.silkframework.workspace

import org.silkframework.util.Identifier

/**
  *
  */
trait RefreshableWorkspaceProvider {
  provider: WorkspaceProvider =>

  /**
    * Refreshes a project, i.e. cleans all possible caches if there are any for this projects and reloads it
    * freshly.
    */
  def refreshProject(project: Identifier): Unit
}
