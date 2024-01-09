package controllers.projectApi.requests

import org.silkframework.workspace.activity.workflow.WorkflowTaskContext
import play.api.libs.json.{Format, Json}

/** Request sending a task context */
case class TaskContextRequest(taskContext: WorkflowTaskContext)

object TaskContextRequest {
  implicit final val taskContextRequestFormat: Format[TaskContextRequest] = Json.format[TaskContextRequest]
}

/** Giving additional information on the input and output tasks in the task context. */
case class TaskContextResponse(inputTasks: Seq[TaskMetaData],
                               outputTasks: Seq[TaskMetaData])

case class TaskMetaData(taskId: String,
                        label: String,
                        isDataset: Boolean,
                        fixedSchema: Boolean)

object TaskContextResponse {
  implicit val inputTaskMetaDataFormat: Format[TaskMetaData] = Json.format[TaskMetaData]
  implicit val taskContextResponseFormat: Format[TaskContextResponse] = Json.format[TaskContextResponse]
}
