package controllers.workspace.taskApi

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, PluginDescription}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonSerializers.TaskSpecJsonFormat
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{Format, JsValue, Json}

@Schema(description = "Request to call a task action.", example = "{}")
case class TaskActionRequest(@Schema(
                               description = "Optional task to call the action on. If not provided, the task will be retrieved from the project.",
                               implementation = classOf[Object], // Dummy type, because JsValue is not recognized as JSON by Swagger
                               requiredMode = RequiredMode.NOT_REQUIRED
                             )
                             task: Option[JsValue]) {

  def call(projectName: String, taskName: String, actionName: String)(implicit user: UserContext): TaskActionResponse = {
    // Retrieve project
    val project = WorkspaceFactory().workspace.project(projectName)

    // Retrieve task
    val taskSpec: TaskSpec =
      task match {
        case Some(taskJson) =>
          // Read provided task
          implicit val readContext: ReadContext = ReadContext.fromProject(project)
          TaskSpecJsonFormat.read(taskJson)
        case None =>
          // Retrieve exiting task
          project.anyTask(taskName)
      }

    // Retrieve plugin spec
    val pluginSpec = PluginDescription.forTask(taskSpec)
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)

    // Call action
    pluginSpec.actions.get(actionName) match {
      case Some(action) =>
        TaskActionResponse(action(taskSpec))
      case None =>
        throw new NotFoundException(s"Action '$actionName' not found on task $taskName in project $projectName.")
    }
  }

}

object TaskActionRequest {
  implicit val format: Format[TaskActionRequest] = Json.format[TaskActionRequest]
}

@Schema(description = "Response of a task action.")
case class TaskActionResponse(@Schema(description = "Optional message as a user-readable markdown.", requiredMode = RequiredMode.NOT_REQUIRED)
                              message: Option[String])

object TaskActionResponse {
  implicit val format: Format[TaskActionResponse] = Json.format[TaskActionResponse]
}
