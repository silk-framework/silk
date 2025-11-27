package controllers.workspace.workspaceRequests

import config.WorkbenchLinks
import controllers.util.ItemType
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{Json, OFormat}

/**
  * Response for copy task(s) endpoints.
  *
  * @param copiedTasks Tasks that are copied to the target project
  * @param overwrittenTasks Tasks that would overwrite existing tasks in the target project
  */
case class CopyTasksResponse(@ArraySchema(
                               schema = new Schema(
                                 description = "Tasks that are copied to the target project.",
                                 implementation = classOf[String]
                             ))
                             copiedTasks: Set[TaskToBeCopied],
                             @ArraySchema(
                               schema = new Schema(
                                 description = "Tasks that would overwrite existing tasks in the target project.",
                                 implementation = classOf[String]
                             ))
                             overwrittenTasks: Set[TaskToBeCopied])

object CopyTasksResponse {
  implicit val jsonFormat: OFormat[CopyTasksResponse] = Json.format[CopyTasksResponse]
}

/**
  * A task that is to be copied to another project.
  *
  * @param taskType The task type label
  * @param id The task id
  * @param label Task label
  * @param originalTaskLink Browser link to the original task
  * @param overwrittenTaskLink Browser link to the overwritten task, if any
  */
case class TaskToBeCopied(pluginId: String, taskType: String, id: String, label: String, originalTaskLink: String, overwrittenTaskLink: Option[String])

object TaskToBeCopied {

  implicit val jsonFormat: OFormat[TaskToBeCopied] = Json.format[TaskToBeCopied]

  def fromTask(task: ProjectTask[_ <: TaskSpec], overwrittenTask: Option[ProjectTask[_ <: TaskSpec]]): TaskToBeCopied = {
    TaskToBeCopied(
      pluginId = PluginDescription.forTask(task).id,
      taskType = ItemType.itemType(task).label,
      id = task.id,
      label = task.label(),
      originalTaskLink = WorkbenchLinks.editorLink(task),
      overwrittenTaskLink = overwrittenTask.map(WorkbenchLinks.editorLink)
    )
  }

}

