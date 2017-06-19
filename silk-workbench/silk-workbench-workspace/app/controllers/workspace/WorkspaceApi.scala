package controllers.workspace

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.net.URL
import java.util.logging.{LogRecord, Logger}

import controllers.core.{Stream, Widgets}
import org.silkframework.config._
import org.silkframework.runtime.activity.{Activity, ActivityControl}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceNotFoundException, UrlResource}
import org.silkframework.runtime.serialization.{ReadContext, Serialization, XmlSerialization}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.workbench.utils.JsonError
import org.silkframework.workspace.activity.{ProjectExecutor, WorkspaceActivity}
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter, WorkspaceIO}
import org.silkframework.workspace._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsArray, JsObject}
import play.api.mvc._

import scala.language.existentials
import scala.concurrent.ExecutionContext.Implicits.global

class WorkspaceApi extends Controller {

  import ProjectMarshallerRegistry._

  def reload: Action[AnyContent] = Action {
    User().workspace.reload()
    Ok
  }

  def projects: Action[AnyContent] = Action {
    Ok(JsonSerializer.projectsJson)
  }

  def getProject(projectName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    Ok(JsonSerializer.projectJson(project))
  }

  def newProject(project: String): Action[AnyContent] = Action {
    if (User().workspace.projects.exists(_.name == project)) {
      Conflict(JsonError(s"Project with name '$project' already exists. Creation failed."))
    } else {
      val projectConfig = ProjectConfig(project)
      projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri))
      val newProject = User().workspace.createProject(projectConfig)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  def deleteProject(project: String): Action[AnyContent] = Action {
    User().workspace.removeProject(project)
    Ok
  }

  def cloneProject(oldProject: String, newProject: String) = Action {
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

  def importProject(project: String): Action[AnyContent] = Action { implicit request =>
    for (data <- request.body.asMultipartFormData;
         file <- data.files) {
      // Read the project from the received file
      val inputStream = new FileInputStream(file.ref.file)
      try {
        val marshaller = marshallerForFile(file.filename)
        val workspace = User().workspace
        workspace.importProject(project, inputStream, marshaller)
      } finally {
        inputStream.close()
      }
    }
    Ok
  }

  /**
    * importProject variant with explicit marshaller parameter
    *
    * @param project
    * @param marshallerId This should be one of the ids returned by the availableProjectMarshallingPlugins method.
    * @return
    */
  def importProjectViaPlugin(project: String, marshallerId: String): Action[AnyContent] = Action { implicit request =>
    val marshallerOpt = marshallerById(marshallerId)
    marshallerOpt match {
      case Some(marshaller) =>
        for (data <- request.body.asMultipartFormData;
             file <- data.files) {
          // Read the project from the received file
          val inputStream = new FileInputStream(file.ref.file)
          try {
            val workspace = User().workspace
            workspace.importProject(project, inputStream, marshaller)
          } finally {
            inputStream.close()
          }
        }
        Ok
      case _ =>
        BadRequest("No plugin '" + marshallerId + "' found for importing project.")
    }
  }

  def exportProject(projectName: String): Action[AnyContent] = Action {
    val marshaller = marshallerById("xmlZip").get
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
  }

  def availableProjectMarshallingPlugins(p: String): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val marshallerOpt = marshallerById(marshallerPluginId)
    marshallerOpt match {
      case Some(marshaller) =>
        // Export the project into a byte array
        val outputStream = new ByteArrayOutputStream()
        val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
        val bytes = outputStream.toByteArray
        outputStream.close()

        Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
      case _ =>
        BadRequest("No plugin '" + marshallerPluginId + "' found for exporting project.")
    }

  }

  def executeProject(projectName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources

    val projectExecutors = PluginRegistry.availablePlugins[ProjectExecutor]
    if (projectExecutors.isEmpty)
      BadRequest("No project executor available")
    else {
      val projectExecutor = projectExecutors.head()
      Activity(projectExecutor.apply(project)).start()
      Ok
    }
  }

  def importLinkSpec(projectName: String): Action[AnyContent] = Action { implicit request => {
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
        UnsupportedMediaType("Link spec must be provided either as Multipart form data or as XML. Please set the Content-Type header accordingly, e.g. to application/xml")
    }
  }
  }

  def exportLinkSpec(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[LinkSpec](taskName)
    implicit val prefixes = project.config.prefixes

    val silkConfig = SilkConfigExporter.build(project, task)

    Ok(XmlSerialization.toXml(silkConfig))
  }

  def updatePrefixes(project: String): Action[AnyContent] = Action { implicit request => {
    val prefixMap = request.body.asFormUrlEncoded.getOrElse(Map.empty).mapValues(_.mkString)
    val projectObj = User().workspace.project(project)
    projectObj.config = projectObj.config.copy(prefixes = Prefixes(prefixMap))

    Ok
  }
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
    val enumerator = Enumerator.fromStream(resource.load)

    Ok.chunked(enumerator).withHeaders("Content-Disposition" -> "attachment")
  }

  def putResource(projectName: String, resourceName: String): Action[AnyContent] = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val resource = project.resources.get(resourceName)

    request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.nonEmpty =>
        try {
          val file = formData.files.head.ref.file
          val inputStream = new FileInputStream(file)
          resource.writeStream(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case AnyContentAsMultipartFormData(formData) if formData.dataParts.contains("resource-url") =>
        try {
          val dataParts = formData.dataParts("resource-url")
          val url = dataParts.head
          val urlResource = UrlResource(new URL(url))
          val inputStream = urlResource.load
          resource.writeStream(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case AnyContentAsRaw(buffer) =>
        val bytes = buffer.asBytes().getOrElse(Array[Byte]())
        resource.writeBytes(bytes)
        Ok
      case _ =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        Ok
    }
  }
  }

  def deleteResource(projectName: String, resourceName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    Ok
  }

  def deleteTask(projectName: String, taskName: String, removeDependentTasks: Boolean): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  def cloneTask(projectName: String, oldTask: String, newTask: String) = Action {
    val project = User().workspace.project(projectName)
    project.addAnyTask(newTask, project.anyTask(oldTask))
    Ok
  }

  def getTaskMetadata(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    Ok(JsonSerializer.taskMetadata(task))
  }
}
