package controllers.workspace

import config.WorkbenchConfig
import config.WorkbenchConfig.WorkspaceReact
import controllers.core.UserContextActions
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.xml.XmlZipProjectMarshaling
import play.api.mvc.{Action, AnyContent, InjectedController}

import java.nio.file.{Files, StandardCopyOption}
import javax.inject.Inject

class WorkspaceController @Inject() (implicit workspaceReact: WorkspaceReact) extends InjectedController with UserContextActions {

  def importExample(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val workspace = WorkspaceFactory().workspace

    // Load example and write it to a temporary file
    val exampleFile = Files.createTempFile("example", ".zip")
    val inputStream = WorkbenchConfig.createConfigResourceLoader.get("example.zip").inputStream

    try {
      Files.copy(inputStream, exampleFile, StandardCopyOption.REPLACE_EXISTING)
      // Import project
      workspace.importProject(Identifier(project), exampleFile.toFile, XmlZipProjectMarshaling())
    } finally {
      // Clean up
      Files.delete(exampleFile)
      inputStream.close()
    }

    Ok
  }
}