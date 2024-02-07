package controllers.projectApi.requests

import org.silkframework.workspace.activity.workflow.WorkflowTaskContext
import play.api.libs.json.{Format, Json}

/** Request sending a task context.
  *
  * @param taskId      The ID of the task whose context this is of.
  * @param taskContext The task context.
  */
case class TaskContextRequest(taskId: String,
                              taskContext: WorkflowTaskContext)

object TaskContextRequest {
  implicit final val taskContextRequestFormat: Format[TaskContextRequest] = Json.format[TaskContextRequest]
}

/** Giving additional information on the input and output tasks in the task context.
  *
  * @param inputTasks      Additional information about each input task.
  * @param outputTasks     Additional information about each output task.
  * @param originalInputs  If the configured input tasks are the same as used in the task context. Even if this is true
  *                        the actual input might still differ, e.g. because of task re-configuration in a workflow etc.
  * @param originalOutputs If the configured output tasks are the same as used in the task context. None if unknown.
  */
case class TaskContextResponse(inputTasks: Seq[TaskMetaData],
                               outputTasks: Seq[TaskMetaData],
                               originalInputs: Option[Boolean],
                               originalOutputs: Option[Boolean])

/** Additional information and characteristics about how a task is used in a specific task context.
  *
  * @param taskId The task ID
  * @param label The task label
  * @param isDataset If the task is a dataset
  * @param fixedSchema If the port of the connected task defines a fixed schema.
  */
case class TaskMetaData(taskId: String,
                        label: String,
                        isDataset: Boolean,
                        fixedSchema: Boolean)

object TaskContextResponse {
  implicit val inputTaskMetaDataFormat: Format[TaskMetaData] = Json.format[TaskMetaData]
  implicit val taskContextResponseFormat: Format[TaskContextResponse] = Json.format[TaskContextResponse]
}
