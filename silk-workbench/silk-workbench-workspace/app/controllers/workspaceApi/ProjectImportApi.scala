package controllers.workspaceApi

import java.io.{File, FileFilter, FileInputStream, FileOutputStream}
import java.time.Instant
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.logging.{Level, Logger}
import java.util.zip.ZipFile
import java.time.{Duration => JDuration}

import config.WorkbenchConfig
import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.workspace.ProjectMarshalingApi
import controllers.workspaceApi.ProjectImportApi.{ProjectImport, ProjectImportDetails, ProjectImportExecution}
import javax.inject.Inject
import org.silkframework.config.{DefaultConfig, MetaData}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.zip.ZipFileResourceLoader
import org.silkframework.runtime.resource.{FileResource, ResourceLoader}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, ConflictRequestException, NotFoundException}
import org.silkframework.util.StreamUtils
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, _}
import org.silkframework.util.DurationConverters._

import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scala.xml.{XML => ScalaXML}

/**
  * API for advanced project import.
  */
class ProjectImportApi @Inject() (api: ProjectMarshalingApi) extends InjectedController with ControllerUtilsTrait {
  private final val PROJECT_FILE_MAX_AGE_KEY = "workspace.projectImport.tempFileMaxAge"
  private final val DEFAULT_PROJECT_FILE_MAX_AGE = Duration(1, TimeUnit.HOURS)

  private val log: Logger = Logger.getLogger(getClass.getName)
  private val tempProjectPrefix = "di-projectImport"

  // Map holding project import objects.
  private val projectsImportQueue = new mutable.HashMap[String, ProjectImport]()

  initialTempCleanUp()

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Execution.createFixedThreadPool(
    "project-import-thread",
    2,
    maxPoolSize = Some(2),
    keepAliveInMs = 1000L
  ))

  /** Execute synchronized code on the projects import queue. */
  private def withProjectImportQueue[T](block: mutable.HashMap[String, ProjectImport] => T): T = {
    projectsImportQueue.synchronized {
      block(projectsImportQueue)
    }
  }

  private def temporaryProjectFileMaxAge: Duration = {
    val cfg = DefaultConfig.instance()
    if(cfg.hasPath(PROJECT_FILE_MAX_AGE_KEY)) {
      cfg.getDuration(PROJECT_FILE_MAX_AGE_KEY).toScala
    } else {
      DEFAULT_PROJECT_FILE_MAX_AGE
    }
  }

  private def initialTempCleanUp(): Unit = {
    Try {
      val tmpDir = System.getProperty("java.io.tmpdir")
      for(file <- new File(tmpDir).listFiles() if file.getName.startsWith(tempProjectPrefix) && file.isFile && file.exists()) {
        Try(file.delete())
      }
    }
  }

  private def isStaleUploadedFile(projectImport: ProjectImport, maxAge: Duration, now: Instant): Boolean = {
    JDuration.between(projectImport.uploadTimeStamp, now).toScala > maxAge &&
        projectImport.projectFileResource.file.exists() &&
    // Do not delete files of running project import executions
        (projectImport.importExecution.isEmpty ||
            projectImport.importExecution.get.importEnded.isDefined)
  }

  private def removeOldTempFiles(): Unit = {
    val now = Instant.now
    val maxAge = temporaryProjectFileMaxAge
    withProjectImportQueue { queue =>
      for((projectImportId, projectImport) <- queue) {
        if(isStaleUploadedFile(projectImport, maxAge, now)) {
          Try(projectImport.projectFileResource.file.delete()) match {
            case Success(_) => log.info(s"Removed temporary file of project import $projectImportId.")
            case Failure(ex) => log.log(Level.WARNING, s"Could not remove temporary file of project import $projectImportId.", ex)
          }
        }
      }
    }
  }

  /** Uploads a project archive and returns the resource URL under which the project import is further handled. */
  def uploadProjectArchiveFile(): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val inputFile = api.bodyAsFile
    removeOldTempFiles()
    val tempFile = File.createTempFile(tempProjectPrefix, "")
    val inputStream = new FileInputStream(inputFile)
    val outputStream = new FileOutputStream(tempFile)
    StreamUtils.fastStreamCopy(inputStream, outputStream, close = true)
    val fileResource = FileResource(tempFile)
    fileResource.setDeleteOnGC(true)
    val id = fileResource.name
    val projectImport = ProjectImport(id, fileResource, Instant.now, None, None)
    withProjectImportQueue(_.put(id, projectImport))
    Created(Json.obj("projectImportId" -> id)).
        withHeaders("Location" -> s"${WorkbenchConfig.applicationContext}/api/workspace/projectImport/$id")
  }

  private def getProjectImport(projectImportId: String): ProjectImport = {
    withProjectImportQueue {
      _.getOrElse(projectImportId,
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
    val pi = getProjectImport(projectImportId)
    pi.synchronized {
      // This might have been updated already
      val newPi = getProjectImport(projectImportId)
      newPi.details match {
        case Some(details) =>
          details
        case None =>
          val details = extractProjectImportDetails(newPi)
          withProjectImportQueue(_.put(projectImportId, newPi.copy(details = Some(details))))
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
        val projectImportError = try {
          val lineIterator = Source.fromInputStream(projectImport.projectFileResource.inputStream).getLines()
          if (lineIterator.hasNext && lineIterator.next().startsWith("@prefix")) {
            // FIXME: Support RDF project import
            ProjectImportApi.errorProjectImportDetails("RDF project import not supported!")
          } else {
            ProjectImportApi.errorProjectImportDetails("No valid project export file detected!")
          }
        } catch {
          case NonFatal(_) =>
            ProjectImportApi.errorProjectImportDetails("No valid project export file detected!")
        }
        Try(projectImport.projectFileResource.delete()) // Not valid, delete.
        projectImportError
    }
  }

  /** Removed a project import and it's temporary files. */
  def removeProjectImport(projectImportId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withProjectImportQueue { queue =>
      queue.get(projectImportId) foreach { pi =>
        pi.projectFileResource.delete()
      }
      queue.remove(projectImportId)
    }
    NoContent
  }

  /** Returns the status of a running project import. The requests will return after a specified timeout. */
  def projectImportExecutionStatus(projectImportId: String, timeout: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val importExecution = withProjectImportQueue(_.get(projectImportId) match {
      case Some(projectImport) if projectImport.importExecution.isDefined =>
        projectImport.importExecution.get
      case None =>
        throw new NotFoundException(s"No started project import found with ID $projectImportId!")
    })
    var responseObj = Json.obj(
      "projectId" -> importExecution.projectId,
      "importStarted" -> importExecution.importStarted.toEpochMilli
    )
    try {
      val result = Await.result(importExecution.importProcess, timeout.millis) // Long polling for the result.
      // Fetch end timestamp if avaialble
      var end: JsValue = JsNull
      withProjectImportQueue { queue =>
        for (projectImport <- queue.get(projectImportId);
             importExecution <- projectImport.importExecution;
             importEnded <- importExecution.importEnded) {
          end = JsNumber(importEnded.toEpochMilli)
        }
      }
      responseObj = responseObj ++ Json.obj("importEnded" -> end)
      if (result.isFailure) {
        responseObj = responseObj ++ Json.obj("failureMessage" -> s"Project import has failed. Details: ${result.failed.get.getMessage}", "success" -> false)
      } else {
        responseObj = responseObj ++ Json.obj("success" -> true)
      }
    } catch {
      case _: TimeoutException | _: InterruptedException =>
    }
    Ok(responseObj)
  }

  /** Starts a project import based on the import ID. */
  def startProjectImport(projectImportId: String,
                         generateNewId: Boolean,
                         overwriteExisting: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    withProjectImportQueue {
      _.get(projectImportId) match {
        case Some(projectImport) =>
          projectImport.importExecution match {
            case None =>
              if(!projectImport.projectFileResource.file.exists()) {
                throw ConflictRequestException(s"The uploaded project file has been purged by the server, because it's max age of ${temporaryProjectFileMaxAge}" +
                    s" seconds has been reached. The max age can be configured via parameter '$PROJECT_FILE_MAX_AGE_KEY'.")
              }
              // Create execution in a future, so this request returns immediately with a 201
              val details = fetchProjectImportDetails(projectImportId)
              var newProjectId = details.projectId
              if (generateNewId) {
                newProjectId = IdentifierUtils.generateProjectId(details.label)
              }
              if(projectExists(newProjectId) && !overwriteExisting) {
                throw ConflictRequestException(s"A project with ID $newProjectId already exists!")
              }
              executeProjectImportAsync(projectImport, details, newProjectId, overwriteExisting)
            case _ =>
          }
          Created.
              withHeaders(LOCATION -> s"${WorkbenchConfig.applicationContext}/api/workspace/projectImport/$projectImportId/status")
        case None =>
          throw NotFoundException(s"Invalid project import ID '$projectImportId'.")
      }
    }
  }

  /** Executes the project import asynchronously. */
  private def executeProjectImportAsync(projectImport: ProjectImport,
                                        details: ProjectImportDetails,
                                        newProjectId: String,
                                        overwriteExisting: Boolean)
                                       (implicit userContext: UserContext): Unit = {
    api.withMarshaller(details.marshallerId) { marshaller =>
      withProjectImportQueue { outerQueue =>
        // Start async import
        val importProcess: Future[Try[Unit]] = Future {
          val result = Try[Unit] {
            workspace.importProject(newProjectId, projectImport.projectFileResource.file, marshaller, overwrite = overwriteExisting)
          }
          // Remove file and update project import object
          withProjectImportQueue { queue =>
            if (result.isSuccess) {
              Try(projectImport.projectFileResource.file.delete())
            }
            queue.get(projectImport.projectImportId) foreach { projectImport =>
              queue.put(projectImport.projectImportId,
                projectImport.copy(importExecution =
                    Some(projectImport.importExecution.get.copy(importEnded = Some(Instant.now)))))
            }
          }
          result
        }
        outerQueue.put(projectImport.projectImportId, projectImport.copy(
          importExecution = Some(ProjectImportExecution(Instant.now, None, newProjectId, importProcess))))
      }
    }
  }
}

object ProjectImportApi {
  final val PROJECT_IMPORT_ID = "projectImportId"

  /**
    * Holds all data regarding a project import actions.
    *
    * @param projectImportId     The ID of this project import.
    * @param projectFileResource The (temporary) project file uploaded by the user.
    * @param uploadTimeStamp     The upload time.
    * @param details             Extracted project details from the project file.
    * @param importExecution     When the actual import is started this keeps track of the execution.
    */
  case class ProjectImport(projectImportId: String,
                           projectFileResource: FileResource,
                           uploadTimeStamp: Instant,
                           details: Option[ProjectImportDetails],
                           importExecution: Option[ProjectImportExecution])


  /**
    * The actual project import execution object.
    * @param importStarted Timestamp when the import was started.
    * @param importEnded   Timestamp when the import has finished.
    * @param importProcess The import process.
    */
  case class ProjectImportExecution(importStarted: Instant,
                                    importEnded: Option[Instant],
                                    projectId: String,
                                    importProcess: Future[Try[Unit]])

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
