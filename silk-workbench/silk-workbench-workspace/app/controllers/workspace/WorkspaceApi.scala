package controllers.workspace

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.net.URL
import java.util.logging.{LogRecord, Logger}

import controllers.core.{Stream, Widgets}
import controllers.util.SerializationUtils
import org.silkframework.config._
import org.silkframework.runtime.activity.{Activity, ActivityControl, SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceNotFoundException, UrlResource}
import org.silkframework.runtime.serialization.{ReadContext, Serialization, XmlSerialization}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.activity.{ProjectExecutor, WorkspaceActivity}
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter, WorkspaceIO}
import org.silkframework.workspace._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsArray, JsBoolean, JsObject}
import play.api.mvc._

import scala.language.existentials
import scala.concurrent.ExecutionContext.Implicits.global

class WorkspaceApi extends Controller {

  def reload: Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    User().workspace.reload()
    Ok
  }

  def projects: Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    Ok(JsonSerializer.projectsJson)
  }

  def getProject(projectName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    Ok(JsonSerializer.projectJson(project))
  }

  def newProject(project: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    if (User().workspace.projects.exists(_.name == project)) {
      ErrorResult(CONFLICT, "Conflict", s"Project with name '$project' already exists. Creation failed.")
    } else {
      val projectConfig = ProjectConfig(project)
      projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri))
      val newProject = User().workspace.createProject(projectConfig)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  def deleteProject(project: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    User().workspace.removeProject(project)
    Ok
  }

  def cloneProject(oldProject: String, newProject: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val workspace = User().workspace
    val project = workspace.project(oldProject)

    val clonedProjectConfig = project.config.copy(id = newProject)
    val clonedProjectUri = clonedProjectConfig.generateDefaultUri
    val clonedProject = workspace.createProject(clonedProjectConfig.copy(projectResourceUriOpt = Some(clonedProjectUri)))
    WorkspaceIO.copyResources(project.resources, clonedProject.resources)
    for(task <- project.allTasks) {
      clonedProject.addAnyTask(task.id, task.data)
    }

    Ok
  }

  def executeProject(projectName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = SimpleUserContext(WebUserManager().user(request))
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources

    val projectExecutors = PluginRegistry.availablePlugins[ProjectExecutor]
    if (projectExecutors.isEmpty) {
      ErrorResult(BadUserInputException("No project executor available"))
    } else {
      val projectExecutor = projectExecutors.head()
      Activity(projectExecutor.apply(project)).start()
      Ok
    }
  }

  def importLinkSpec(projectName: String): Action[AnyContent] = Action { implicit request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    implicit val readContext = ReadContext(project.resources)

    request.body match {
      case AnyContentAsMultipartFormData(data) =>
        for (file <- data.files) {
          val config = XmlSerialization.fromXml[LinkingConfig](scala.xml.XML.loadFile(file.ref.file))
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

  def exportLinkSpec(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes

    val silkConfig = SilkConfigExporter.build(project, task)

    Ok(XmlSerialization.toXml(silkConfig))
  }

  def updatePrefixes(project: String): Action[AnyContent] = Action { implicit request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = User().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }

  def getResources(projectName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)

    Ok(JsonSerializer.projectResources(project))
  }

  def getResourceMetadata(projectName: String, resourcePath: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val resource = project.resources.getInPath(resourcePath, File.separatorChar)

    val pathPrefix = resourcePath.lastIndexOf(File.separatorChar) match {
      case -1 => ""
      case index => resourcePath.substring(0, index + 1)
    }

    Ok(JsonSerializer.resourceProperties(resource, pathPrefix))
  }

  def getResource(projectName: String, resourceName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName, mustExist = true)
    val enumerator = Enumerator.fromStream(resource.inputStream)

    Ok.chunked(enumerator).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String): Action[AnyContent] = Action { implicit request =>
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName)

    request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.nonEmpty =>
        try {
          val file = formData.files.head.ref.file
          resource.writeFile(file)
          NoContent
        } catch {
          case ex: Exception =>
            ErrorResult(BadUserInputException(ex))
        }
      case AnyContentAsMultipartFormData(formData) if formData.dataParts.contains("resource-url") =>
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
      case AnyContentAsMultipartFormData(formData) if formData.files.isEmpty =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        NoContent
      case AnyContentAsRaw(buffer) =>
        val bytes = buffer.asBytes().getOrElse(Array[Byte]())
        resource.writeBytes(bytes)
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
  }

  def deleteResource(projectName: String, resourceName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    NoContent
  }
}
