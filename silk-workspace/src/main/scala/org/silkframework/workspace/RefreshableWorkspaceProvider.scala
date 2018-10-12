package org.silkframework.workspace

/**
  *
  */
trait RefreshableWorkspaceProvider {
  provider: WorkspaceProvider =>

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  def refresh(): Unit
}
