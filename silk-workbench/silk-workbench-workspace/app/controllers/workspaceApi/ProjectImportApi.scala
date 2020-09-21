package controllers.workspaceApi

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.ZipFile

import config.WorkbenchConfig
import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.workspace.{JsonSerializer, ProjectMarshalingApi}
import controllers.workspaceApi.ProjectImportApi.{ProjectImport, ProjectImportDetails}
import javax.inject.Inject
import org.silkframework.config.MetaData
import org.silkframework.runtime.resource.zip.ZipFileResourceLoader
import org.silkframework.runtime.resource.{FileResource, ResourceLoader}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.StreamUtils
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, _}

import scala.collection.mutable
import scala.io.Source
import scala.util.Try
import scala.xml.{XML => ScalaXML}

/**
  * API for advanced project import.
  */
class ProjectImportApi @Inject() (api: ProjectMarshalingApi) extends InjectedController with ControllerUtilsTrait {
  // Map holding project import objects. TODO: Purge old project imports to not fill up the FS
  private val projectsImportQueue = new mutable.HashMap[String, ProjectImport]()

  /** Uploads a project archive and returns the resource URL under which the project import is further handled. */
  def uploadProjectArchiveFile(): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val inputFile = api.bodyAsFile
    val tempFile = File.createTempFile("di-projectImport", ".zip")
    val inputStream = new FileInputStream(inputFile)
    val outputStream = new FileOutputStream(tempFile)
    StreamUtils.fastStreamCopy(inputStream, outputStream, close = true)
    val fileResource = FileResource(tempFile)
    fileResource.setDeleteOnGC(true)
    val id = fileResource.name
    val projectImport = ProjectImport(fileResource, System.currentTimeMillis(), None)
    projectsImportQueue.synchronized {
      projectsImportQueue.put(id, projectImport)
    }
    Created(Json.obj("projectImportId" -> id)).
        withHeaders("Location" -> s"${WorkbenchConfig.applicationContext}/api/workspace/projectImport/$id")
  }

  private def projectImport(projectImportId: String): ProjectImport = {
    projectsImportQueue.synchronized {
      projectsImportQueue.getOrElse(projectImportId,
        throw NotFoundException(s"Invalid project import ID '$projectImportId'."))
    }
  }

  /** Returns the project import details. */
  def projectImportDetails(projectImportId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val details = fetchProjectImportDetails(projectImportId)
    val projectExists = if(details.projectId != "") {
      workspace.findProject(details.projectId).isDefined
    } else { false }
    Ok(Json.toJson(details.copy(projectAlreadyExists = projectExists)))
  }

  // Returns the project import details. Caches the result.
  private def fetchProjectImportDetails(projectImportId: String): ProjectImportDetails = {
    val pi = projectImport(projectImportId)
    pi.synchronized {
      // This might have been updated already
      val newPi = projectImport(projectImportId)
      newPi.details match {
        case Some(details) =>
          details
        case None =>
          val details = extractProjectImportDetails(newPi)
          projectsImportQueue.synchronized {
            projectsImportQueue.put(projectImportId, newPi.copy(details = Some(details)))
          }
          details
      }
    }
  }

  private def extractProjectImportDetails(projectImport: ProjectImport): ProjectImportDetails = {
    try {
      val configFile =  "config.xml"
      val zipFile = new ZipFile(projectImport.projectFileResource.file)
      var resourceLoader: ResourceLoader = new ZipFileResourceLoader(zipFile, "")
      if(!resourceLoader.list.contains(configFile)) {
        if (resourceLoader.listChildren.nonEmpty) {
          resourceLoader = resourceLoader.child(resourceLoader.listChildren.head)
          if(!resourceLoader.list.contains(configFile)) {
            throw new NotFoundException("No project found in given zip file. Imported nothing.")
          }
        } else {
          throw new NotFoundException("No project found in given zip file. Imported nothing.")
        }
      }
      val xml = ScalaXML.load(resourceLoader.get(configFile).inputStream)
      implicit val readContext: ReadContext = ReadContext()
      val prefixes = (xml \ "Prefixes").headOption
      if(prefixes.isEmpty) {
        throw new BadUserInputException("Invalid project XML format. config.xml does not contain Prefixes section.")
      }
      val resourceUri = (xml \ "@resourceUri").text
      val projectId = resourceUri.split("[/#:]").last
      val metaData = (xml \ "MetaData").headOption.map(MetaData.MetaDataXmlFormat.read).getOrElse(
        MetaData(projectId)
      )
      ProjectImportDetails(projectId, metaData.label, metaData.description, XmlZipWithResourcesProjectMarshaling.marshallerId,
        projectAlreadyExists = false, None)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        val lineIterator = Source.fromInputStream(projectImport.projectFileResource.inputStream).getLines()
        if(lineIterator.hasNext && lineIterator.next().startsWith("@prefix")) {
          // TODO: Support RDF project import
          ProjectImportApi.errorProjectImportDetails("RDF project import not supported!")
        } else {
          ProjectImportApi.errorProjectImportDetails("No valid project export file detected!")
        }
    }
  }

  /** Removed a project import and it's temporary files. */
  def removeProjectImport(projectImportId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    projectsImportQueue.synchronized {
      projectsImportQueue.get(projectImportId) foreach { pi =>
        pi.projectFileResource.delete()
      }
      projectsImportQueue.remove(projectImportId)
    }
    NoContent
  }

  /** Starts a project import based on the import ID. */
  def startProjectImport(projectImportId: String, generateNewId: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    projectsImportQueue.synchronized {
      projectsImportQueue.get(projectImportId)
    } match {
      case Some(projectImport) =>
        val details = fetchProjectImportDetails(projectImportId)
        var newProjectId = details.projectId
        if(generateNewId) {
          newProjectId = IdentifierUtils.generateProjectId(details.label)
        }
        api.withMarshaller(details.marshallerId) { marshaller =>
          workspace.importProject(newProjectId, projectImport.projectFileResource.file, marshaller)
          // Delete file and remove from queue
          projectsImportQueue.synchronized {
            Try(projectImport.projectFileResource.file.delete())
            projectsImportQueue.remove(projectImportId)
          }
          val project = workspace.project(newProjectId)
          Created(JsonSerializer.projectJson(project)).
              withHeaders(LOCATION -> s"${WorkbenchConfig.applicationContext}/api/workspace/projects/$newProjectId")
        }
      case None =>
        throw NotFoundException(s"Invalid project import ID '$projectImportId'.")
    }
  }

}

object ProjectImportApi {
  final val PROJECT_IMPORT_ID = "projectImportId"
  case class ProjectImport(projectFileResource: FileResource, uploadTimeStamp: Long, details: Option[ProjectImportDetails])

  /**
    * The details for the project import.
    *
    * @param projectId            The ID of the project from the project archive.
    * @param label                The label of the project.
    * @param description          The description of the project.
    * @param marshallerId         The ID of the detected marshaller that has been used for exporting the project.
    * @param projectAlreadyExists True if a project with the same ID already exists in the workspace.
    * @param errorMessage         An error was encountered and the import cannot proceed.
    *                             This error message should include all information for the user, other fields should be ignored
    *                             by the client.
    */
  case class ProjectImportDetails(projectId: String,
                                  label: String,
                                  description: Option[String],
                                  marshallerId: String,
                                  projectAlreadyExists: Boolean,
                                  errorMessage: Option[String])

  def errorProjectImportDetails(errorMessage: String): ProjectImportDetails = {
    ProjectImportDetails("", "", None, "", projectAlreadyExists = false, Some(errorMessage))
  }

  implicit val projectImportDetailsFormat: Format[ProjectImportDetails] = Json.format[ProjectImportDetails]
}
