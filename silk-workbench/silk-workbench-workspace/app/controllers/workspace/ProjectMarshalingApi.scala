package controllers.workspace

import java.io.{ByteArrayOutputStream, FileInputStream, InputStream}

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.{ProjectMarshallerRegistry, ProjectMarshallingTrait, User}
import play.api.libs.json.JsArray
import play.api.mvc._


class ProjectMarshalingApi extends Controller {

  import ProjectMarshallerRegistry._

  def availableMarshallingPlugins(): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
  }

  def importProject(project: String): Action[AnyContent] = Action { implicit request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    try {
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
  def importProjectViaPlugin(project: String, marshallerId: String): Action[AnyContent] = Action { implicit request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    withMarshaller(marshallerId) { marshaller =>
      withBodyAsStream { inputStream =>
        val workspace = User().workspace
        workspace.importProject(project, inputStream, marshaller)
        Ok
      }
    }
  }

  def exportProject(projectName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val marshaller = marshallerById("xmlZip").get
    // Export the project into a byte array
    val outputStream = new ByteArrayOutputStream()
    val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
    val bytes = outputStream.toByteArray
    outputStream.close()

    Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
  }

  def exportProjectViaPlugin(projectName: String, marshallerPluginId: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    withMarshaller(marshallerPluginId) { marshaller =>
      // Export the project into a byte array
      val outputStream = new ByteArrayOutputStream()
      val fileName = User().workspace.exportProject(projectName, outputStream, marshaller)
      val bytes = outputStream.toByteArray
      outputStream.close()

      Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
    }
  }

  def importWorkspaceViaPlugin(marshallerId: String): Action[AnyContent] = Action { implicit request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    User().workspace.clear()
    withMarshaller(marshallerId) { marshaller =>
      withBodyAsStream { inputStream =>
        val workspace = User().workspace
        marshaller.unmarshalWorkspace(workspace.provider, workspace.repository, inputStream)
        workspace.reload()
        Ok
      }
    }
  }

  def exportWorkspaceViaPlugin(marshallerPluginId: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    withMarshaller(marshallerPluginId) { marshaller =>
      val outputStream = new ByteArrayOutputStream()
      val workspace = User().workspace
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
