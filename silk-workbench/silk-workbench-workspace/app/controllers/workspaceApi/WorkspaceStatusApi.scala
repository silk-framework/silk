package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.errorReporting.ErrorReport.ErrorReportItem
import controllers.workspaceApi.WorkspaceStatusApi.{ProjectFailedTasks, WorkspaceStatus}
import controllers.workspaceApi.project.ProjectLoadingErrors
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * Provides aggregate status information about the whole workspace.
  */
@Tag(name = "Workspace")
class WorkspaceStatusApi extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Workspace status",
    description = "Returns aggregate status information about the workspace. Currently this lists all tasks that " +
      "could not be loaded, grouped by project, together with summary counts. Only projects that have at least one " +
      "failed task are included in the 'projects' array.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[WorkspaceStatus])
        ))
      )
    ))
  def status(): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val projects = workspace.userProjects
    val failingProjects =
      for {
        project <- projects
        loadingErrors = project.loadingErrors
        if loadingErrors.nonEmpty
      } yield {
        ProjectFailedTasks(
          projectId = project.id.toString,
          projectLabel = project.config.metaData.label,
          failedTaskCount = loadingErrors.size,
          failedTasks = loadingErrors.map(ProjectLoadingErrors.fromTaskLoadingError)
        )
      }
    val status = WorkspaceStatus(
      projectCount = projects.size,
      failedProjectCount = failingProjects.size,
      failedTaskCount = failingProjects.map(_.failedTaskCount).sum,
      projects = failingProjects
    )
    Ok(Json.toJson(status))
  }
}

object WorkspaceStatusApi {

  @Schema(description = "The failed tasks of a single project.")
  case class ProjectFailedTasks(@Schema(description = "The identifier of the project.")
                                projectId: String,
                                @Schema(description = "The label of the project, if defined.", nullable = true)
                                projectLabel: Option[String],
                                @Schema(description = "The number of tasks in this project that could not be loaded.")
                                failedTaskCount: Int,
                                @ArraySchema(schema = new Schema(
                                  description = "The detailed loading error reports of the failed tasks.",
                                  implementation = classOf[ErrorReportItem]
                                ))
                                failedTasks: Seq[ErrorReportItem])

  object ProjectFailedTasks {
    implicit val projectFailedTasksFormat: Format[ProjectFailedTasks] = Json.format[ProjectFailedTasks]
  }

  @Schema(description = "Aggregate workspace status, currently focused on tasks that failed to load.")
  case class WorkspaceStatus(@Schema(description = "The total number of projects the user has access to.")
                             projectCount: Int,
                             @Schema(description = "The number of projects that have at least one failed task.")
                             failedProjectCount: Int,
                             @Schema(description = "The total number of failed tasks across all projects.")
                             failedTaskCount: Int,
                             @ArraySchema(schema = new Schema(
                               description = "The failed tasks grouped by project. Only contains projects that have at least one failed task.",
                               implementation = classOf[ProjectFailedTasks]
                             ))
                             projects: Seq[ProjectFailedTasks])

  object WorkspaceStatus {
    implicit val workspaceStatusFormat: Format[WorkspaceStatus] = Json.format[WorkspaceStatus]
  }
}
