package controllers.workspace

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.net.URL
import java.util.logging.{LogRecord, Logger}

import controllers.core.{Stream, Widgets}
import models.JsonError
import org.silkframework.config._
import org.silkframework.runtime.activity.{Activity, ActivityControl}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceNotFoundException, UrlResource}
import org.silkframework.runtime.serialization.{ReadContext, Serialization, XmlSerialization}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.{LinkSpec, LinkingConfig}
import org.silkframework.workspace.activity.{ProjectExecutor, WorkspaceActivity}
import org.silkframework.workspace.io.{SilkConfigExporter, SilkConfigImporter}
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, ProjectTask, User}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsArray
import play.api.mvc._

import scala.language.existentials
import scala.concurrent.ExecutionContext.Implicits.global

object WorkspaceApi extends Controller {

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
      val newProject = User().workspace.createProject(project)
      Created(JsonSerializer.projectJson(newProject))
    }
  }

  def deleteProject(project: String): Action[AnyContent] = Action {
    User().workspace.removeProject(project)
    Ok
  }

  def importProject(project: String): Action[AnyContent] = Action { implicit request =>
    for (data <- request.body.asMultipartFormData;
         file <- data.files) {
      // Read the project from the received file
      val inputStream = new FileInputStream(file.ref.file)
      val dotIndex = file.filename.lastIndexOf('.')
      if (dotIndex < 0) {
        throw new IllegalArgumentException("No recognizable file name suffix in uploaded file.")
      }
      val suffix = file.filename.substring(dotIndex + 1)
      val marshallers = marshallingPluginsByFileHandler()
      try {
        marshallers.get(suffix) match {
          case Some(marshaller) =>
            val marshaller = marshallers(suffix)
            val workspace = User().workspace
            workspace.importProject(project, inputStream, marshaller)
          case _ =>
            throw new IllegalArgumentException("No handler found for " + suffix + " files")
        }
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
    val marshallerOpt = marshallingPlugins().filter(_.id == marshallerId).headOption
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

  def marshallingPluginsByFileHandler(): Map[String, ProjectMarshallingTrait] = {
    marshallingPlugins().map { mp =>
      mp.suffix.map(s => (s, mp))
    }.flatten.toMap
  }

  def marshallingPlugins(): Seq[ProjectMarshallingTrait] = {
    implicit val prefixes = Prefixes.empty
    implicit val resources = EmptyResourceManager
    val pluginConfigs = PluginRegistry.availablePluginsForClass(classOf[ProjectMarshallingTrait])
    pluginConfigs.map(pc =>
      PluginRegistry.create[ProjectMarshallingTrait](pc.id)
    )
  }

  def exportProject(projectName: String): Action[AnyContent] = Action {
    val marshallers = marshallingPlugins()
    val marshaller = marshallers.filter(_.id == "xmlZip").head
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
  }

  def availableProjectMarshallingPlugins(p: String): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins()
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val marshallerOpt = marshallingPlugins().filter(_.id == marshallerPluginId).headOption
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

    val silkConfig = SilkConfigExporter.build(project, task.task)

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
          resource.write(inputStream)
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
          resource.write(inputStream)
          inputStream.close()
          Ok
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case AnyContentAsRaw(buffer) =>
        val bytes = buffer.asBytes().getOrElse(Array[Byte]())
        resource.write(bytes)
        Ok
      case _ =>
        // Put empty resource
        resource.write(Array[Byte]())
        Ok
    }
  }
  }

  def deleteResource(projectName: String, resourceName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    project.resources.delete(resourceName)

    Ok
  }
}