package controllers.workspace

import akka.stream.scaladsl.StreamConverters
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.FileMultiPartRequest
import controllers.workspace.ProjectMarshalingApi._
import controllers.workspace.doc.ProjectMarshalingApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.execution.Execution
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import org.silkframework.workspace.{ProjectMarshallerRegistry, ProjectMarshallingTrait, WorkspaceFactory}
import play.api.libs.json.JsArray
import play.api.mvc._

import java.io._
import java.util.logging.Logger
import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Tag(name = "Project import/export", description = "Import and export projects.")
class ProjectMarshalingApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  import ProjectMarshallerRegistry._

  @Operation(
    summary = "Marshalling plugins",
    description = "Returns a list of supported workspace/project import/export plugins, e.g. RDF, XML.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(ProjectMarshalingApiDoc.marshallingPluginsExample))
          )
        )
      )
    )
  )
  def availableMarshallingPlugins(): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def importProject(project: String): Action[AnyContent] = importProjectViaPlugin(project, XmlZipWithResourcesProjectMarshaling.marshallerId)

  @Operation(
    summary = "Import project",
    description = "Import a project from the file send with the request.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the import succeeded"
      )
    )
  )
  @RequestBody(
    description = "The project file to be imported.",
    required = true,
    content = Array(
      new Content(
        mediaType = "multipart/form-data",
        schema = new Schema(implementation = classOf[FileMultiPartRequest])
      ),
      new Content(
        mediaType = "application/octet-stream"
      ),
    )
  )
  def importProjectViaPlugin(@Parameter(
                               name = "project",
                               description = "The project identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             project: String,
                             @Parameter(
                               name = "importPlugin",
                               description = "The marshalling format. One of the ids returned from the `marshallingPlugins` endpoint.",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    withMarshaller(marshallerId) { marshaller =>
      val workspace = WorkspaceFactory().workspace
      workspace.importProject(project, bodyAsFile, marshaller)
      Ok
    }
  }

  def exportProject(projectName: String): Action[AnyContent] = exportProjectViaPlugin(projectName, XmlZipWithResourcesProjectMarshaling.marshallerId)

  @Operation(
    summary = "Export project",
    description = "Export a project with the specified marshalling plugin.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The response contains the exported project."
      )
    )
  )
  def exportProjectViaPlugin(@Parameter(
                               name = "project",
                               description = "The project identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             projectName: String,
                             @Parameter(
                               name = "exportPlugin",
                               description = "The marshalling format. One of the ids returned from the `marshallingPlugins` endpoint.",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withMarshaller(marshallerPluginId) { marshaller =>
      val source = createSource { outputStream =>
        WorkspaceFactory().workspace.exportProject(projectName, outputStream, marshaller)
      }
      val fileName = projectName + marshaller.suffix.map("." + _).getOrElse("")
      Ok.chunked(source).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
    }
  }

  @Operation(
    summary = "Import workspace",
    description = "Imports the entire workspace from the file send with the request. Before importing all existing projects will be removed from the workspace.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the import succeeded"
      )
    )
  )
  @RequestBody(
    description = "The workspace file to be imported.",
    required = true,
    content = Array(
      new Content(
        mediaType = "multipart/form-data",
        schema = new Schema(implementation = classOf[FileMultiPartRequest])
      ),
      new Content(
        mediaType = "application/octet-stream"
      ),
    )
  )
  def importWorkspaceViaPlugin(@Parameter(
                                 name = "importPlugin",
                                 description = "The marshalling format. One of the ids returned from the `marshallingPlugins` endpoint.",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    WorkspaceFactory().workspace.clear()
    withMarshaller(marshallerId) { marshaller =>
      val workspace = WorkspaceFactory().workspace
      marshaller.unmarshalWorkspace(workspace.provider, workspace.repository, bodyAsFile)
      workspace.reload()
      log.info(s"Imported workspace. " + userContext.logInfo)
      Ok
    }
  }

  @Operation(
    summary = "Export workspace",
    description = "Exports the entire workspace with the specified marshalling plugin.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The response contains the exported workspace."
      )
    )
  )
  def exportWorkspaceViaPlugin(@Parameter(
                                 name = "exportPlugin",
                                 description = "The marshalling format. One of the ids returned from the `marshallingPlugins` endpoint.",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withMarshaller(marshallerPluginId) { marshaller =>
      val source = createSource { outputStream =>
        val workspace = WorkspaceFactory().workspace
        marshaller.marshalWorkspace(outputStream, workspace.projects, workspace.repository)
      }
      val fileName = "workspace" + marshaller.suffix.map("." + _).getOrElse("")
      Ok.chunked(source).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
    }
  }

  def withMarshaller[T](marshallerId: String)(f: ProjectMarshallingTrait => T): T = {
    marshallerById(marshallerId) match {
      case Some(marshaller) =>
        f(marshaller)
      case None =>
        throw BadUserInputException("No marshaller plugin '" + marshallerId + "' found. Available marshallers: " + marshallingPlugins.map(_.id).mkString(", "))
    }
  }

  private def createSource(serializeFunc: java.io.OutputStream => Unit) = {
    StreamConverters.fromInputStream(() => {
      val outputStream = new PipedOutputStream
      val pipedInputStream = new PipedInputStream(outputStream)

      exportExecutionContext.execute(() => {
        try {
          serializeFunc(outputStream)
        } finally {
          outputStream.close()
        }
      })

      pipedInputStream
    })
  }

  def bodyAsFile(implicit request: Request[AnyContent]): File = {
    request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.size == 1 =>
        formData.files.head.ref.path.toFile
      case AnyContentAsMultipartFormData(formData) if formData.files.size != 1 =>
        throw BadUserInputException("Must provide exactly one file in multipart form data body.")
      case AnyContentAsRaw(buffer) =>
        buffer.asFile
      case _ =>
        throw BadUserInputException("Must attach body as multipart/form-data or as raw bytes.")
    }
  }

}

object ProjectMarshalingApi {

  /**
    * To prevent deadlock, all project exports are executed in a separate thread pool.
    */
  private val exportExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Execution.createThreadPool("ProjectMarshalingApi"))

}
