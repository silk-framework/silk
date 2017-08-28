package controllers.workspace

import java.io.{ByteArrayOutputStream, FileInputStream}

import org.silkframework.workspace.{ProjectMarshallerRegistry, User}
import play.api.libs.json.JsArray
import play.api.mvc.{Action, AnyContent, Controller}

class ProjectMarshalingApi extends Controller {

  import ProjectMarshallerRegistry._

  def availableMarshallingPlugins(): Action[AnyContent] = Action {
    val marshaller = marshallingPlugins
    Ok(JsArray(marshaller.map(JsonSerializer.marshaller)))
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
        BadRequest("No plugin '" + marshallerPluginId + "' found for exporting workspace.")
    }

  }

  def importWorkspaceViaPlugin(marshallerId: String): Action[AnyContent] = Action { implicit request =>
    val marshallerOpt = marshallerById(marshallerId)
    marshallerOpt match {
      case Some(marshaller) =>
        for (data <- request.body.asMultipartFormData;
             file <- data.files) {
          // Read the project from the received file
          val inputStream = new FileInputStream(file.ref.file)
          try {
            val workspace = User().workspace
            marshaller.unmarshalWorkspace(workspace.provider, workspace.repository, inputStream)
            workspace.reload()
          } finally {
            inputStream.close()
          }
        }
        Ok
      case _ =>
        BadRequest("No plugin '" + marshallerId + "' found for importing workspace.")
    }
  }

  def exportWorkspaceViaPlugin(marshallerPluginId: String): Action[AnyContent] = Action {
    val marshallerOpt = marshallerById(marshallerPluginId)
    marshallerOpt match {
      case Some(marshaller) =>
        val outputStream = new ByteArrayOutputStream()
        val workspace = User().workspace
        val fileName = marshaller.marshalWorkspace(outputStream, workspace.provider, workspace.repository)
        val bytes = outputStream.toByteArray
        outputStream.close()

        Ok(bytes).withHeaders("Content-Disposition" -> s"attachment; filename=$fileName")
      case _ =>
        BadRequest("No plugin '" + marshallerPluginId + "' found for exporting project.")
    }

  }

}
