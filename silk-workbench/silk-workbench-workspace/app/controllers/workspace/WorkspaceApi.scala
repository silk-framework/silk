package controllers.workspace

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.WorkspaceApiDoc
import controllers.workspace.workspaceRequests.{CopyTasksRequest, CopyTasksResponse, UpdateGlobalVocabularyRequest}
import controllers.workspaceApi.project.ProjectLoadingErrors
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config._
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace._
import org.silkframework.workspace.activity.ProjectExecutor
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter, WorkspaceIO}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import javax.inject.Inject
import scala.language.existentials
import scala.util.Try

@Tag(name = "Projects")
class WorkspaceApi  @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Reload",
    description = "Reloads the workspace from the backend. The request blocks until the reload finished.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reload succeeded."
      )
    )
  )
  def reload: Action[AnyContent] = UserContextAction { implicit userContext =>
    val workspace = WorkspaceFactory().workspace
    workspace.reload()
    for(project <- workspace.projects) {
      ProjectLoadingErrors.tryReloadTasks(project)
    }
    Ok
  }

  @Operation(
    summary = "Reload workspace prefixes",
    description = "Reloads the workspace prefixes from registered or all vocabularies from the backend. The request blocks until the reload finished.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the reload succeeded."
      )
    )
  )
  def reloadPrefixes: Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.reloadPrefixes()
    Ok
  }

  @Operation(
    summary = "List projects",
    description = "Get a list with all projects.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(WorkspaceApiDoc.projectListExample))
        ))
      )
    )
  )
  def projects: Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(JsonSerializer.projectsJson)
  }

  @Operation(
    summary = "Get project contents",
    description = "List the contents of a single project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(WorkspaceApiDoc.projectExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project has not been found."
      )
    )
  )
  def getProject(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    accessMonitor.saveProjectAccess(project.config.id)
    Ok(JsonSerializer.projectJson(project))
  }

  @Operation(
    summary = "Create project",
    description = "Create a new empty project.",
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject("{\"name\": \"project name\"}"))
        ))
      ),
      new ApiResponse(
        responseCode = "409",
        description = "If a project with the specified identifier already exists."
      )
    )
  )
  def newProject(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    if (WorkspaceFactory().workspace.projects.exists(_.id.toString == project)) {
      ErrorResult(CONFLICT, "Conflict", s"Project with name '$project' already exists. Creation failed.")
    } else {
      val projectConfig = ProjectConfig(project, metaData = MetaData(Some(project)).asNewMetaData)
      projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri))
      val newProject = WorkspaceFactory().workspace.createProject(projectConfig)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  @Operation(
    summary = "Delete project",
    description = "Delete a project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the project has been deleted successfully."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project has not been found."
      )
    )
  )
  def deleteProject(@Parameter(
                      name = "project",
                      description = "The project identifier",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.removeProject(project)
    Ok
  }

  @Operation(
    summary = "Clone project",
    description = "Clone a project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the project has been cloned successfully."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the source project has not been found."
      )
    )
  )
  def cloneProject(@Parameter(
                     name = "project",
                     description = "The identifier of the source project, which is to be cloned.",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   oldProject: String,
                   @Parameter(
                     name = "newProject",
                     description = "The identifier of the cloned project.",
                     required = true,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   newProject: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val workspace = WorkspaceFactory().workspace
    val project = workspace.project(oldProject)

    val clonedProjectConfig = project.config.copy(id = newProject)
    val clonedProjectUri = clonedProjectConfig.generateDefaultUri
    val clonedProject = workspace.createProject(clonedProjectConfig.copy(projectResourceUriOpt = Some(clonedProjectUri)))
    WorkspaceIO.copyResources(project.resources, clonedProject.resources)
    // Clone tags
    for (tag <- project.tagManager.allTags()) {
      clonedProject.tagManager.putTag(tag)
    }
    // Clone tasks, since task specs may contain state, e.g. RDF file dataset
    for (task <- project.allTasks) {
      val taskParameters = task.data.parameters(PluginContext.fromProject(project))
      val clonedTaskSpec = task.data.withParameters(taskParameters, dropExistingValues = true)(PluginContext.fromProject(clonedProject))
      clonedProject.addAnyTask(task.id, clonedTaskSpec, task.metaData.asNewMetaData)
    }

    Ok
  }

  @Operation(
    summary = "Copy project tasks",
    description = "Copies all tasks in a project to another project. Referenced resources are copied only if the target project uses a different resource path than the source project.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Lists all copied tasks.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[CopyTasksResponse]),
            examples = Array(new ExampleObject(WorkspaceApiDoc.copyProjectResponse))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        schema = new Schema(implementation = classOf[CopyTasksRequest]),
        examples = Array(new ExampleObject(WorkspaceApiDoc.copyProjectRequest))
      )
    )
  )
  def copyProject(@Parameter(
                    name = "project",
                    description = "The project identifier.",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>implicit userContext =>
    validateJson[CopyTasksRequest] { copyRequest =>
      val result = copyRequest.copyProject(projectName)
      Ok(Json.toJson(result))
    }
  }

  @Operation(
    summary = "Reload a project from the workspace provider",
    description = "Reloads all tasks of a project from the workspace provider. This is the same as the workspace reload, but for only a single project.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project has not been found."
      )
    )
  )
  def reloadProject(@Parameter(
                      name = "project",
                      description = "The project identifier.",
                      required = true,
                      in = ParameterIn.PATH,
                      schema = new Schema(implementation = classOf[String])
                    )
                    projectId: String): Action[AnyContent] = RequestUserContextAction { implicit request =>
    implicit userContext =>
      val workspace = WorkspaceFactory().workspace
      workspace.reloadProject(Identifier(projectId))
      ProjectLoadingErrors.tryReloadTasks(workspace.project(projectId))
      NoContent
  }

  def executeProject(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val context: PluginContext = PluginContext.fromProject(project)

    val projectExecutors = PluginRegistry.availablePlugins[ProjectExecutor]
    if (projectExecutors.isEmpty) {
      ErrorResult(BadUserInputException("No project executor available"))
    } else {
      val projectExecutor = projectExecutors.head()
      Activity(projectExecutor.apply(project)).start()
      Ok
    }
  }

  def importLinkSpec(projectName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)

    request.body match {
      case AnyContentAsMultipartFormData(data) =>
        for (file <- data.files) {
          val config = XmlSerialization.fromXml[LinkingConfig](scala.xml.XML.loadFile(file.ref.path.toFile))
          SilkConfigImporter(config, project)
        }
        Ok
      case AnyContentAsXml(xml) =>
        val config = XmlSerialization.fromXml[LinkingConfig](xml.head)
        SilkConfigImporter(config, project)
        Ok
      case _ =>
        ErrorResult(UnsupportedMediaTypeException.supportedFormats("multipart/form-data", "application/xml"))
    }
  }

  def exportLinkSpec(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    val silkConfig = SilkConfigExporter.build(project, task)

    Ok(XmlSerialization.toXml(silkConfig))
  }

  def updatePrefixes(project: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).view.mapValues(_.mkString).toMap
    val projectObj = WorkspaceFactory().workspace.project(project)
    projectObj.config = projectObj.config.copy(projectPrefixes = Prefixes(prefixMap))

    Ok
  }

  @Operation(
    summary = "Update global vocabulary cache",
    description = "Update a specific vocabulary of the global vocabulary cache. This request is non-blocking. It can take a while for the cache to be up to date.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "The update of the vocabulary cache has been scheduled."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        schema = new Schema(implementation = classOf[UpdateGlobalVocabularyRequest]),
        examples = Array(new ExampleObject("""{ "iri": "http://xmlns.com/foaf/0.1/" }"""))
      )
    )
  )
  def updateGlobalVocabularyCache(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext =>
      validateJson[UpdateGlobalVocabularyRequest] { updateRequest =>
        GlobalVocabularyCache.putVocabularyInQueue(updateRequest.iri)
        val activityControl = workspace.activity[GlobalVocabularyCache].control
        if(!activityControl.status.get.exists(_.isRunning)) {
          Try(activityControl.start())
        }
        NoContent
      }
  }
}
