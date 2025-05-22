package controllers.workspaceApi

import config.WorkbenchConfig
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.FileMultiPartRequest
import controllers.workspace.ProjectMarshalingApi
import controllers.workspaceApi.ProjectImportApi.{ProjectImport, ProjectImportDetails, ProjectImportExecution}
import io.swagger.v3.oas.annotations.enums.{ParameterIn, ParameterStyle}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{DefaultConfig, MetaData, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.resource.zip.ZipFileResourceLoader
import org.silkframework.runtime.resource.{EmptyResourceManager, FileResource, ResourceLoader}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.validation.{BadUserInputException, ConflictRequestException, NotFoundException, RequestException}
import org.silkframework.util.DurationConverters._
import org.silkframework.util.{IdentifierUtils, StreamUtils}
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json._
import play.api.mvc._

import java.io.{File, FileInputStream, FileOutputStream}
import java.time.{Instant, Duration => JDuration}
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.logging.{Level, Logger}
import java.util.zip.ZipFile
import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.{Codec, Source}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scala.xml.{XML => ScalaXML}

/**
  * API for advanced project import.
  */
@Tag(name = "Project import/export")
class ProjectImportApi @Inject() (api: ProjectMarshalingApi) extends InjectedController with UserContextActions with ControllerUtilsTrait {
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
  @Operation(
    summary = "Project import resources",
    description = "Project import resources are used for a multi step project import procedure that is comprised of multiple steps: 1. the project file upload, 2. the validation of the uploaded file, 3. the asynchronous execution of the project import and 4. the status of the running project import execution.",
    parameters = Array(
      new Parameter(
        name = "file",
        description = "The file to be uploaded",
        style = ParameterStyle.FORM,
        content = Array(new Content(mediaType = "multipart/form-data"), new Content(mediaType = "application/octet-stream"), new Content(mediaType = "text/plain"))
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "A project import resource with the returned ID has been created.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject("{ \"projectImportId\": \"di-projectImport5196140007678722748\" }"))
        ))
      )
    ))
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "multipart/form-data",
        schema = new Schema(implementation = classOf[FileMultiPartRequest])
      ),
      new Content(
        mediaType = "application/octet-stream"
      ),
      new Content(
        mediaType = "text/plain"
      )
    )
  )
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
  @Operation(
    summary = "Project import details",
    description = "The project import resource that was created by uploading a project file.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Details for the uploaded project file. The label, description and projectId properties were extracted from the uploaded file. The projectAlreadyExists property states that there already exists a project with the exact same ID. In that case special flags can be set for the subsequent request that starts the project import execution. The marshallerId is the detected project file format, in the example it is a project XML ZIP archive.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject("{ \"description\": \"Config project description\", \"label\": \"Config Project\", \"marshallerId\": \"xmlZip\", \"projectAlreadyExists\": true, \"projectId\": \"configProject\" }"))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the provided import identifier does not exist."
      )
    ))
  def projectImportDetails(@Parameter(
                             name = "projectImportId",
                             description = "The project import id.",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectImportId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
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
      val configFile = "config.xml"
      val zipFile = new ZipFile(projectImport.projectFileResource.file)
      var resourceLoader: ResourceLoader = new ZipFileResourceLoader(zipFile, "")
      if(!resourceLoader.list.contains(configFile)) {
        if (resourceLoader.listChildren.size == 1) {
          resourceLoader = resourceLoader.child(resourceLoader.listChildren.head)
          if(!resourceLoader.list.contains(configFile)) {
            throw new NotFoundException("No project folder found in given zip file. Imported nothing.")
          }
        } else if(resourceLoader.listChildren.size > 1) {
          throw BadUserInputException("Invalid project export file or workspace export file with multiple projects.")
        } else {
          throw new NotFoundException("No project found in given zip file. Imported nothing.")
        }
      }
      val xml = ScalaXML.load(resourceLoader.get(configFile).inputStream)
      val prefixes = (xml \ "Prefixes").headOption
      if(prefixes.isEmpty) {
        throw new BadUserInputException("Invalid project XML format. config.xml does not contain Prefixes section.")
      }
      val projectId = (xml \ "@resourceUri").text.split("[/#:]").last
      // Empty read context only used for MetaData
      implicit val readContext: ReadContext = ReadContext(EmptyResourceManager(), Prefixes.empty)
      val metaData = (xml \ "MetaData").headOption.map(MetaData.MetaDataXmlFormat.read).getOrElse(MetaData.empty)
      ProjectImportDetails(projectId, metaData.label.getOrElse(projectId), metaData.description, XmlZipWithResourcesProjectMarshaling.marshallerId,
        projectAlreadyExists = false, None)
    } catch {
      case ex: RequestException =>
        Try(projectImport.projectFileResource.delete())
        ProjectImportApi.errorProjectImportDetails(ex.getMessage)
      case ex: Exception =>
        handleUnexpectedError(projectImport, ex)
    }
  }

  private def handleUnexpectedError(projectImport: ProjectImport, ex: Exception): ProjectImportDetails = {
    log.log(Level.INFO, s"Failed to import project $projectImport", ex)
    val projectImportError = try {
      val lineIterator = Source.fromInputStream(projectImport.projectFileResource.inputStream)(Codec.UTF8).getLines()
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

  /** Removed a project import and it's temporary files. */
  @Operation(
    summary = "Remove project import resource",
    description = "Deletes the project import resource and the uploaded files. The delete request is idempotent.",
    responses = Array(
      new ApiResponse(
        responseCode = "201"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the provided import identifier does not exist."
      )
    ))
  def removeProjectImport(@Parameter(
                            name = "projectImportId",
                            description = "The project import id.",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectImportId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withProjectImportQueue { queue =>
      queue.get(projectImportId) foreach { pi =>
        pi.projectFileResource.delete()
      }
      queue.remove(projectImportId)
    }
    NoContent
  }

  /** Returns the status of a running project import. The requests will return after a specified timeout. */
  @Operation(
    summary = "Project import execution status",
    description = "When the project import execution has been started, this will return the status of the project execution.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The status of the project execution. There are 3 types of responses: 1. The execution is still in progress, i.e. no 'success' property is defined. 2. The 'success' property is defined and set to true, which means that the import has been successful. 3. The 'success' property is defined and set to false, which means that the import has failed. The 'failureMessage' property gives the reason for the failure.",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject("{ \"projectId\": \"1e813497-0c75-48cf-a857-2ddc3f94fe26_ConfigProject\", \"importStarted\": 1600950697304, \"importEnded\": 1600950697497, \"success\": false, \"failureMessage\": \"Exception during...\" }")))
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "The execution has not been started, yet, or the project import ID is not known."
      )
    ))
  def projectImportExecutionStatus(@Parameter(
                                     name = "projectImportId",
                                     description = "The project import id.",
                                     required = true,
                                     in = ParameterIn.PATH,
                                     schema = new Schema(implementation = classOf[String])
                                   )
                                   projectImportId: String,
                                   @Parameter(
                                     name = "timeout",
                                     description = "The timeout in milliseconds when this call should return if the execution is not finished, yet. This allow for long-polling the result.",
                                     required = false,
                                     in = ParameterIn.QUERY,
                                     schema = new Schema(implementation = classOf[Int])
                                   )
                                   timeout: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val importExecution = withProjectImportQueue(_.get(projectImportId) match {
      case Some(projectImport) if projectImport.importExecution.isDefined =>
        projectImport.importExecution.get
      case _ =>
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
  @Operation(
    summary = "Start project import",
    description = "Starts a project import based on the import identifier.",
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "The project import has been executed. The status of the project import can be requested via the status endpoint.",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject("{ \"projectId\": \"1e813497-0c75-48cf-a857-2ddc3f94fe26_ConfigProject\", \"importStarted\": 1600950697304, \"importEnded\": 1600950697497, \"success\": false, \"failureMessage\": \"Exception during...\" }")))
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "The execution has not been started, yet, or the project import ID is not known."
      ),
      new ApiResponse(
        responseCode = "409",
        description = "Returned if a project with the same ID already exists and neither generateNewId nor overwriteExisting is enabled. Also returned if the uploaded temporary project file has been deleted because it reached its max age."
      )
    ))
  def startProjectImport(@Parameter(
                           name = "projectImportId",
                           description = "The project import id.",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         projectImportId: String,
                         @Parameter(
                           name = "generateNewId",
                           description = "When enabled this will always generate a new ID for this project based on the project label. This is one strategy if a project with the original ID already exists.",
                           required = false,
                           in = ParameterIn.QUERY,
                           schema = new Schema(implementation = classOf[Boolean])
                         )
                         generateNewId: Boolean,
                         @Parameter(
                           name = "overwriteExisting",
                           description = "When enabled this will overwrite an existing project with the same ID. Enabling this option will NOT override the generateNewId option.",
                           required = false,
                           in = ParameterIn.QUERY,
                           schema = new Schema(implementation = classOf[Boolean])
                         )
                         overwriteExisting: Boolean,
                         @Parameter(
                           name = "newProjectId",
                           description = "If provided, this will adopt the given id for the imported project. Cannot be set together with 'generateNewId'.",
                           required = false,
                           in = ParameterIn.QUERY,
                           schema = new Schema(implementation = classOf[String])
                         )
                         newProjectId: Option[String]): Action[AnyContent] = UserContextAction { implicit userContext =>
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
              // Determine new project id
              val projectId = (newProjectId, generateNewId) match {
                case (None, false) => details.projectId
                case (Some(newId), false) => newId
                case (None, true) => IdentifierUtils.generateProjectId(details.label).toString
                case (Some(_), true) =>
                  throw new BadUserInputException("'generateNewId' and 'newProjectId' are mutually exclusive and cannot be set at the same time.")
              }
              if(projectExists(projectId) && !overwriteExisting) {
                throw ConflictRequestException(s"A project with ID $projectId already exists!")
              }
              executeProjectImportAsync(projectImport, details, projectId, overwriteExisting)
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
