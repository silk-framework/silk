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
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.Json
import play.api.mvc._
import resources.ResourceHelper

import java.io.File
import java.net.URLConnection
import java.util.logging.Logger
import javax.inject.Inject

@Tag(name = "Project resources", description = "Manage file resources in a project.")
class ResourceApi  @Inject() extends InjectedController with UserContextActions with ControllerUtilsTrait {

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
  def getFileMetadata(@Parameter(
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
    val resource = project.resources.getInPath(resourcePath, mustExist = true)

    val pathPrefix = resourcePath.lastIndexOf(File.separatorChar) match {
      case -1 => ""
      case index => resourcePath.substring(0, index + 1)
    }

    Ok(JsonSerializer.resourceProperties(resource, pathPrefix))
  }

  @deprecated("Use files-endpoints instead.")
  def getResourceMetadata(projectName: String, resourceName: String): Action[AnyContent] = getFileMetadata(projectName, resourceName).apply()

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
  def getFile(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
              @Parameter(
                    name = "path",
                    description = "The resource path relative to the resource repository",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName, mustExist = true)
    val contentType = Option(URLConnection.guessContentTypeFromName(resourceName))
    Ok.chunked(StreamConverters.fromInputStream(() => resource.inputStream), contentType).withHeaders("Content-Disposition" -> s"""attachment; filename="${resource.name}"""")
  }

  @deprecated("Use files-endpoints instead.")
  def getResource(projectName: String, resourceName: String): Action[AnyContent] = getFile(projectName, resourceName).apply()

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
  def putFile(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
              @Parameter(
                    name = "path",
                    description = "The resource path relative to the resource repository",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  resourceName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName)
    ResourceHelper.uploadResource(project, resource)
  }

  @deprecated("Use files-endpoints instead.")
  def putResource(projectName: String, resourceName: String): Action[AnyContent] = putFile(projectName, resourceName).apply()

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
  def fileUsage(@Parameter(
                      name = "project",
                      description = "The project identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    projectId: String,
                @Parameter(
                      name = "path",
                      description = "The file path relative to the resource repository",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    filePath: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = super[ControllerUtilsTrait].getProject(projectId)
    val resource = project.resources.getInPath(filePath)
    val dependentTasks: Seq[TaskLinkInfo] = ResourceHelper.tasksDependingOnResource(resource, project)
      .map { task =>
        TaskLinkInfo(task.id, task.fullLabel, PluginApiCache.taskTypeByClass(task.taskType))
      }
    Ok(Json.toJson(dependentTasks))
  }

  @deprecated("Use files-endpoints instead.")
  def resourceUsage(projectName: String, resourceName: String): Action[AnyContent] = fileUsage(projectName, resourceName).apply()

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
  def deleteFile(@Parameter(
                       name = "project",
                       description = "The project identifier",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                 @Parameter(
                       name = "path",
                       description = "The resource path relative to the resource repository",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     path: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    deleteRecursive(project.resources, path.split("/").toSeq)
    log.info(s"Deleted resource '$path' in project '$projectName'. " + userContext.logInfo)
    NoContent
  }

  @deprecated("Use files-endpoints instead.")
  def deleteResource(projectName: String, resourceName: String): Action[AnyContent] = deleteFile(projectName, resourceName).apply()

  /**
    * Deletes a file path. Empty folders are removed as well.
    */
  private def deleteRecursive(resources: ResourceManager, pathSegments: Seq[String]): Unit = {
    pathSegments match {
      case Seq(name) =>
        resources.delete(name)
      case folderName +: tail =>
        val folder = resources.child(folderName)
        deleteRecursive(folder, tail)
        if(folder.list.isEmpty && folder.listChildren.isEmpty) {
          resources.delete(folderName)
        }
    }
  }

}
