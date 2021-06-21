package controllers.workspace

import akka.stream.scaladsl.StreamConverters
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.ProjectMarshalingApi._
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

class ProjectMarshalingApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  import ProjectMarshallerRegistry._

  def availableMarshallingPlugins(): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def importProject(project: String): Action[AnyContent] = importProjectViaPlugin(project, XmlZipWithResourcesProjectMarshaling.marshallerId)

  /**
    * importProject variant with explicit marshaller parameter
    *
    * @param project
    * @param marshallerId This should be one of the ids returned by the availableProjectMarshallingPlugins method.
    * @return
    */
  def importProjectViaPlugin(project: String, marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    withMarshaller(marshallerId) { marshaller =>
      val workspace = WorkspaceFactory().workspace
      workspace.importProject(project, bodyAsFile, marshaller)
      Ok
    }
  }

  def exportProject(projectName: String): Action[AnyContent] = exportProjectViaPlugin(projectName, XmlZipWithResourcesProjectMarshaling.marshallerId)

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withMarshaller(marshallerPluginId) { marshaller =>
      val source = createSource { outputStream =>
        WorkspaceFactory().workspace.exportProject(projectName, outputStream, marshaller)
      }
      val fileName = projectName + marshaller.suffix.map("." + _).getOrElse("")
      Ok.chunked(source).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
    }
  }

  def importWorkspaceViaPlugin(marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    WorkspaceFactory().workspace.clear()
    withMarshaller(marshallerId) { marshaller =>
      val workspace = WorkspaceFactory().workspace
      marshaller.unmarshalWorkspace(workspace.provider, workspace.repository, bodyAsFile)
      workspace.reload()
      log.info(s"Imported workspace. " + userContext.logInfo)
      Ok
    }
  }

  def exportWorkspaceViaPlugin(marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
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
        throw BadUserInputException("Must attach body as multipart form data or as raw bytes.")
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
