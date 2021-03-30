package controllers.workspace.workspaceRequests

case class CopyTasksResponse(copiedTasks: Set[String], overwrittenTasks: Set[String])
