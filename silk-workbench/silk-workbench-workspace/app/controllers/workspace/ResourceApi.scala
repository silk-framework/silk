package controllers.workspace

import akka.stream.scaladsl.StreamConverters
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.ResourceApiDoc
import controllers.workspace.doc.ResourceApiDoc.ResourceMultiPartRequest
import controllers.workspace.workspaceApi.TaskLinkInfo
import controllers.workspaceApi.coreApi.PluginApiCache
import controllers.workspaceApi.search.ResourceSearchRequest
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.resource.{UrlResource, WritableResource}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._

import java.io.File
import java.net.URL
import java.util.logging.Logger
import javax.inject.Inject

@Tag(name = "Project resources", description = "Manage file resources in a project.")
class ResourceApi  @Inject() (pluginApiCache: PluginApiCache) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  @Operation(
    summary = "List project resources",
    description = "Lists all resources available to a specific project. Resources of a project are for example files used as input or output.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ResourceApiDoc.resourceListExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      )
    )
  )
  def getResources(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "searchText",
                     description = "If defined the resources will be filtered by the search text which searches over the resource names.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   searchText: Option[String],
                   @Parameter(
                     name = "limit",
                     description = "Limits the number of resources returned by this endpoint.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[Int])
                   )
                   limit: Option[Int],
                   @Parameter(
                     name = "offset",
                     description = "The offset in the result list. Offset and limit allow paging over the results.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[Int])
                   )
                   offset: Option[Int]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val resourceRequest = ResourceSearchRequest(searchText, limit, offset)
    val project = WorkspaceFactory().workspace.project(projectName)
    Ok(resourceRequest(project))
  }

  @Operation(
    summary = "Retrieve resource metadata",
    description = "Retrieve the properties of a specific resource.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(ResourceApiDoc.resourceMetadataExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or resource has not been found."
      )
    )
  )
  def getResourceMetadata(@Parameter(
                            name = "project",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectName: String,
                          @Parameter(
                            name = "name",
                            description = "The resource",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          resourcePath: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.getInPath(resourcePath, File.separatorChar, mustExist = true)

    val pathPrefix = resourcePath.lastIndexOf(File.separatorChar) match {
      case -1 => ""
      case index => resourcePath.substring(0, index + 1)
    }

    Ok(JsonSerializer.resourceProperties(resource, pathPrefix))
  }

  @Operation(
    summary = "Retrieve resource",
    description = "Retrieve the contents of a resource.",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or resource has not been found."
      )
    )
  )
  def getResource(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "name",
                    description = "The resource",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName, mustExist = true)
    Ok.chunked(StreamConverters.fromInputStream(() => resource.inputStream)).withHeaders("Content-Disposition" -> "attachment")
  }

  @Operation(
    summary = "Upload resource",
    description = ResourceApiDoc.resourceUploadDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "If the resource has been uploaded successfully."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or resource has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "multipart/form-data",
        schema = new Schema(
          implementation = classOf[ResourceMultiPartRequest]
        )
      ),
      new Content(
        mediaType = "application/octet-stream"
      ),
      new Content(
        mediaType = "text/plain"
      ),
    )
  )
  def putResource(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "name",
                    description = "The resource",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  resourceName: String): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName)

    val response = request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.nonEmpty =>
        putResourceFromMultipartFormData(resource, formData)
      case AnyContentAsMultipartFormData(formData) if formData.dataParts.contains("resource-url") =>
        putResourceFromResourceUrl(resource, formData)
      case AnyContentAsMultipartFormData(formData) if formData.files.isEmpty =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        NoContent
      case AnyContentAsRaw(buffer) =>
        resource.writeFile(buffer.asFile)
        NoContent
      case AnyContentAsText(txt) =>
        resource.writeString(txt)
        NoContent
      case AnyContentAsEmpty =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        NoContent
      case _ =>
        ErrorResult(UnsupportedMediaTypeException.supportedFormats("multipart/form-data", "application/octet-stream", "text/plain"))
    }
    if(response == NoContent) { // Successfully updated
      log.info(s"Created/updated resource '$resourceName' in project '$projectName'. " + userContext.logInfo)
    }
    response
  }

  private def putResourceFromMultipartFormData(resource: WritableResource, formData: MultipartFormData[Files.TemporaryFile]) = {
    try {
      val file = formData.files.head.ref.path.toFile
      resource.writeFile(file)
      NoContent
    } catch {
      case ex: Exception =>
        ErrorResult(BadUserInputException(ex))
    }
  }

  private def putResourceFromResourceUrl(resource: WritableResource, formData: MultipartFormData[Files.TemporaryFile]): Result = {
    try {
      val dataParts = formData.dataParts("resource-url")
      val url = dataParts.head
      val urlResource = UrlResource(new URL(url))
      resource.writeResource(urlResource)
      NoContent
    } catch {
      case ex: Exception =>
        ErrorResult(BadUserInputException(ex))
    }
  }

  @Operation(
    summary = "Resource usage",
    description = "Returns a list of datasets/tasks that are using the specified resource.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject("[\"dataset 1\", \"dataset 2\"]"))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or resource has not been found."
      )
    )
  )
  def resourceUsage(@Parameter(
                      name = "project",
                      description = "The project identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    projectId: String,
                    @Parameter(
                      name = "name",
                      description = "The resource",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = super[ControllerUtilsTrait].getProject(projectId)
    val dependentTasks: Seq[TaskLinkInfo] = project.allTasks
      .filter(_.referencedResources.map(_.name).contains(resourceName))
      .map { task =>
        TaskLinkInfo(task.id, task.fullTaskLabel, pluginApiCache.taskTypeByClass(task.taskType))
      }
    Ok(Json.toJson(dependentTasks))
  }

  @Operation(
    summary = "Delete resource",
    description = "If the resource exists, delete it.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "If the resource has been deleted. Also returns 204 if the resource does not exist."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      )
    )
  )
  def deleteResource(@Parameter(
                       name = "project",
                       description = "The project identifier",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                     @Parameter(
                       name = "name",
                       description = "The resource",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.resources.delete(resourceName)
    log.info(s"Deleted resource '$resourceName' in project '$projectName'. " + userContext.logInfo)
    NoContent
  }

}
