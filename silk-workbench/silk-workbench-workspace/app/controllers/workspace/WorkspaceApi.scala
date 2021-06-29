package controllers.workspace

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.workspaceRequests.{CopyTasksRequest, UpdateGlobalVocabularyRequest}
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config._
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.BadUserInputException
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

@Tag(name = "Workspace")
class WorkspaceApi  @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController with UserContextActions with ControllerUtilsTrait {

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
