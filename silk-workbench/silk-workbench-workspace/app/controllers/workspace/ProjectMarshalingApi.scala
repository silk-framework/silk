package controllers.workspace

import java.io.{ByteArrayOutputStream, FileInputStream, InputStream}

import controllers.core.{RequestUserContextAction, UserContextAction}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.{ProjectMarshallerRegistry, ProjectMarshallingTrait, WorkspaceFactory}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsArray
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectMarshalingApi extends Controller {

  import ProjectMarshallerRegistry._

  def availableMarshallingPlugins(): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def importProject(project: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    try {
      for (data <- request.body.asMultipartFormData;
           file <- data.files) {
        // Read the project from the received file
        val inputStream = new FileInputStream(file.ref.file)
        try {
          val marshaller = marshallerForFile(file.filename)
          val workspace = WorkspaceFactory().workspace
          workspace.importProject(project, inputStream, marshaller)
        } finally {
          inputStream.close()
        }
      }
      Ok
    } catch {
      case ex: ValidationException =>
        BadRequest(ex.getMessage)
    }
  }

  /**
    * importProject variant with explicit marshaller parameter
    *
    * @param project
    * @param marshallerId This should be one of the ids returned by the availableProjectMarshallingPlugins method.
    * @return
    */
  def importProjectViaPlugin(project: String, marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    withMarshaller(marshallerId) { marshaller =>
      withBodyAsStream { inputStream =>
        val workspace = WorkspaceFactory().workspace
        workspace.importProject(project, inputStream, marshaller)
        Ok
      }
    }
  }

  def exportProject(projectName: String): Action[AnyContent] = exportProjectViaPlugin(projectName, "xmlZip")

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withMarshaller(marshallerPluginId) { marshaller =>
      val enumerator = Enumerator.outputStream { outputStream =>
        val fileName = WorkspaceFactory().workspace.exportProject(projectName, outputStream, marshaller)
        outputStream.close()
      }

      Ok.chunked(enumerator).withHeaders("Content-Disposition" -> s"attachment; filename=export.zip")
    }
  }

  def importWorkspaceViaPlugin(marshallerId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    WorkspaceFactory().workspace.clear()
    withMarshaller(marshallerId) { marshaller =>
      withBodyAsStream { inputStream =>
        val workspace = WorkspaceFactory().workspace
        marshaller.unmarshalWorkspace(workspace.provider, workspace.repository, inputStream)
        workspace.reload()
        Ok
      }
    }
  }

  def exportWorkspaceViaPlugin(marshallerPluginId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    withMarshaller(marshallerPluginId) { marshaller =>
      val outputStream = new ByteArrayOutputStream()
      val workspace = WorkspaceFactory().workspace
      val fileName = marshaller.marshalWorkspace(outputStream, workspace.provider, workspace.repository)
      val bytes = outputStream.toByteArray
      outputStream.close()

      Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
    }
  }

  def withMarshaller(marshallerId: String)(f: ProjectMarshallingTrait => Result): Result = {
    marshallerById(marshallerId) match {
      case Some(marshaller) =>
        f(marshaller)
      case None =>
        throw BadUserInputException("No marshaller plugin '" + marshallerId + "' found. Available marshallers: " + marshallingPlugins.map(_.id).mkString(", "))
    }
  }

  def withBodyAsStream(f: InputStream => Result)(implicit request: Request[AnyContent]): Result = {
    request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.size == 1 =>
        val inputStream = new FileInputStream(formData.files.head.ref.file)
        try {
          f(inputStream)
        } finally {
          inputStream.close()
        }
      case AnyContentAsMultipartFormData(formData) if formData.files.size != 1 =>
        throw BadUserInputException("Must provide exactly one file in multipart form data body.")
      case AnyContentAsRaw(buffer) =>
        val inputStream = new FileInputStream(buffer.asFile)
        try {
          f(inputStream)
        } finally {
          inputStream.close()
        }
      case _ =>
        throw BadUserInputException("Must attach body as multipart form data or as raw bytes.")
    }
  }

}
