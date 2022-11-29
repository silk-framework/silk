package controllers.workspace

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.ResourceBasedDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.FileResource
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import java.io.File
import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Tag(name = "Project tasks")
class TaskDownloadApi @Inject() extends InjectedController with UserContextActions with ControllerUtilsTrait {

  implicit private lazy val executionContext: ExecutionContext = controllerComponents.executionContext

  @Operation(
    summary = "Download info",
    description = "Checks if this task supports downloading its result",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Information if the result of a task can be downloaded.",
        content = Array(new Content(schema = new Schema(implementation = classOf[DownloadInfo])))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  def downloadInfo(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "task",
                     description = "The task identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val info =
      retrieveDownloadFile(projectName, taskName) match {
        case Right(_) =>
          DownloadInfo(downloadSupported = true, info = "Download result of this task.")
        case Left(message) =>
          DownloadInfo(downloadSupported = false, info = message)
      }
    Ok(Json.toJson(info))
  }

  @Operation(
    summary = "Task output",
    description = "Downloads the contents of the first output dataset of the specified task. Note that this does not execute the task, but assumes that it has been executed already. The output dataset must be file based.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The task output."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the output could not be downloaded. The reason is stated in the response body."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or task does not exist."
      )
    )
  )
  def downloadOutput(@Parameter(
                       name = "project",
                       description = "The project identifier",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                     @Parameter(
                       name = "task",
                       description = "The task identifier",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    retrieveDownloadFile(projectName, taskName) match {
      case Right(file) =>
        Ok.sendFile(file)
      case Left(message) =>
        ErrorResult(BAD_REQUEST, "Cannot download result", message)
    }
  }

  /**
    * Retrieves the download file of a task.
    * If the task result cannot be downloaded, an explanation is returned instead.
    */
  private def retrieveDownloadFile(projectId: Identifier, taskId: Identifier)
                                  (implicit user: UserContext): Either[String, File] = {
    val project = WorkspaceFactory().workspace.project(projectId)
    val task = project.anyTask(taskId)

    task.data.outputTasks.headOption match {
      case Some(outputId) =>
        project.taskOption[GenericDatasetSpec](outputId).map(_.data.plugin) match {
          case Some(ds: ResourceBasedDataset) =>
            ds.file match {
              case FileResource(file) =>
                if(file.exists()) {
                  Right(file)
                } else {
                  Left(s"Download not possible. File has not been written yet.")
                }
              case _ =>
                Left(s"Download not possible. The specified output dataset '$outputId' is not based on a file resource.")
            }
          case Some(_) =>
            Left(s"Download not possible. The specified output dataset '$outputId' is not based on a resource.")
          case None =>
            Left(s"Download not possible. The specified output dataset '$outputId' has not been found.")
        }
      case None =>
        Left("Download not possible. This task does not specify an output dataset.")
    }
  }

  @Schema(description = "Information on whether the result of a task can be downloaded.")
  case class DownloadInfo(@Schema(
                            description = "True, if the result of this task can be downloaded. False, otherwise.",
                            required = true)
                          downloadSupported: Boolean,
                          @Schema(
                            description = "User-readable explanation why the result cannot be downloaded.",
                            required = true)
                          info: String)

  implicit val downloadInfoFormat: Format[DownloadInfo] = Json.format[DownloadInfo]

}
