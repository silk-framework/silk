package controllers.workspaceApi.coreApi.variableTemplate

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.config.TaskSpec
import org.silkframework.workspace.{ProjectTask, WorkbenchLinks}
import play.api.libs.json.{Format, Json}

@Schema(description = "A reference to a task.")
case class TaskReferenceJson(@Schema(description = "The task identifier.", requiredMode = RequiredMode.REQUIRED)
                             id: String,
                             @Schema(description = "The task label.", requiredMode = RequiredMode.NOT_REQUIRED)
                             label: Option[String],
                             @Schema(description = TaskReferenceJson.taskTypeDescription, requiredMode = RequiredMode.REQUIRED)
                             taskType: String)

object TaskReferenceJson {

  /** The vocabulary of [[WorkbenchLinks.taskType]]. */
  final val taskTypeDescription = "The task type: 'dataset', 'transform', 'linking', 'ruleBlock', 'workflow', or 'task' for any other task."

  implicit val taskReferenceFormat: Format[TaskReferenceJson] = Json.format[TaskReferenceJson]

  def fromTask(task: ProjectTask[_ <: TaskSpec]): TaskReferenceJson = {
    TaskReferenceJson(task.id, task.metaData.label, WorkbenchLinks.taskType(task))
  }
}
