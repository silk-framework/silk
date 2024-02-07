package controllers.util

import controllers.projectApi.requests.{TaskContextRequest, TaskContextResponse}
import controllers.projectApi.routes.ProjectTaskApi
import helper.ApiClient
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.WorkflowTaskContext

trait ProjectTaskApiClient extends ApiClient {
  def taskContext(projectId: Identifier, taskId: String, taskContext: WorkflowTaskContext): TaskContextResponse = {
    val request = TaskContextRequest(taskId, taskContext)
    postRequest[TaskContextRequest, TaskContextResponse](ProjectTaskApi.taskContext(projectId), request)
  }
}
