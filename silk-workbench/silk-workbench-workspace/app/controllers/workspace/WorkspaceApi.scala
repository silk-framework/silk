package controllers.workspace

import akka.stream.scaladsl.Source
import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.workspace.workspaceApi.TaskLinkInfo
import controllers.workspace.workspaceRequests.{CopyTasksRequest, CopyTasksResponse, UpdateGlobalVocabularyRequest}
import controllers.workspaceApi.coreApi.PluginApiCache
import controllers.workspaceApi.search.ResourceSearchRequest
import org.silkframework.config._
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{ResourceManager, UrlResource, WritableResource}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import org.silkframework.workspace._
import org.silkframework.workspace.activity.ProjectExecutor
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter, WorkspaceIO}
import play.api.libs.Files
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import java.io.File
import java.net.URL
import java.util.logging.Logger
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.existentials
import scala.util.Try

class WorkspaceApi  @Inject() (accessMonitor: WorkbenchAccessMonitor, pluginApiCache: PluginApiCache) extends InjectedController with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def reload: Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.reload()
    Ok
  }

  def reloadPrefixes: Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.reloadPrefixes()
    Ok
  }

  def projects: Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(JsonSerializer.projectsJson)
  }

  def getProject(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    accessMonitor.saveProjectAccess(project.config.id)
    Ok(JsonSerializer.projectJson(project))
  }

  def newProject(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    if (WorkspaceFactory().workspace.projects.exists(_.name.toString == project)) {
      ErrorResult(CONFLICT, "Conflict", s"Project with name '$project' already exists. Creation failed.")
    } else {
      val projectConfig = ProjectConfig(project, metaData = MetaData(project).asNewMetaData)
      projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri))
      val newProject = WorkspaceFactory().workspace.createProject(projectConfig)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  def deleteProject(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.removeProject(project)
    Ok
  }

  def cloneProject(oldProject: String, newProject: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val workspace = WorkspaceFactory().workspace
    val project = workspace.project(oldProject)

    val clonedProjectConfig = project.config.copy(id = newProject)
    val clonedProjectUri = clonedProjectConfig.generateDefaultUri
    val clonedProject = workspace.createProject(clonedProjectConfig.copy(projectResourceUriOpt = Some(clonedProjectUri)))
    WorkspaceIO.copyResources(project.resources, clonedProject.resources)
    // Clone task spec, since task specs may contain state, e.g. RDF file dataset
    implicit val resourceManager: ResourceManager = project.resources
    implicit val prefixes: Prefixes = project.config.prefixes
    for(task <- project.allTasks) {
      val clonedTaskSpec = Try(task.data.withProperties(Map.empty)).getOrElse(task.data)
      clonedProject.addAnyTask(task.id, clonedTaskSpec, task.metaData.asNewMetaData)
    }

    Ok
  }

  def copyProject(projectName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>implicit userContext =>
    implicit val jsonReader = Json.reads[CopyTasksRequest]
    implicit val jsonWriter = Json.writes[CopyTasksResponse]
    validateJson[CopyTasksRequest] { copyRequest =>
      val result = copyRequest.copyProject(projectName)
      Ok(Json.toJson(result))
    }
  }

  def executeProject(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources

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
    implicit val readContext: ReadContext = ReadContext(project.resources)

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
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = WorkspaceFactory().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }

  def getResources(projectName: String,
                   searchText: Option[String],
                   limit: Option[Int],
                   offset: Option[Int]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val resourceRequest = ResourceSearchRequest(searchText, limit, offset)
    val project = WorkspaceFactory().workspace.project(projectName)
    Ok(resourceRequest(project))
  }

  def getResourceMetadata(projectName: String, resourcePath: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.getInPath(resourcePath, File.separatorChar, mustExist = true)

    val pathPrefix = resourcePath.lastIndexOf(File.separatorChar) match {
      case -1 => ""
      case index => resourcePath.substring(0, index + 1)
    }

    Ok(JsonSerializer.resourceProperties(resource, pathPrefix))
  }

  def getResource(projectName: String, resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName, mustExist = true)
    val enumerator = Enumerator.fromStream(resource.inputStream)
    val source = Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))

    Ok.chunked(source).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String): Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
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

  /** The list of tasks that use this resource. */
  def resourceUsage(projectId: String, resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = super[ControllerUtilsTrait].getProject(projectId)
    val dependentTasks: Seq[TaskLinkInfo] = project.allTasks
        .filter(_.referencedResources.map(_.name).contains(resourceName))
        .map { task =>
          TaskLinkInfo(task.id, task.taskLabel(Int.MaxValue), pluginApiCache.taskTypeByClass(task.taskType))
        }
    Ok(Json.toJson(dependentTasks))
  }

  def deleteResource(projectName: String, resourceName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    project.resources.delete(resourceName)
    log.info(s"Deleted resource '$resourceName' in project '$projectName'. " + userContext.logInfo)
    NoContent
  }

  /** Updates the global vocabulary cache for a specific vocabulary */
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
