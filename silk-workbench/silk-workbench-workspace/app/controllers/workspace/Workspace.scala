package controllers.workspace

import config.WorkbenchConfig
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.xml.{XmlZipProjectMarshaling, XmlWorkspaceProvider}
import org.silkframework.workspace.{PrefixRegistry, User}
import play.api.mvc.{Action, Controller}

object Workspace extends Controller {

  def index = Action {
    Ok(views.html.workspace.workspace())
  }

  def tree = Action {
    Ok(views.html.workspace.workspaceTree(User().workspace))
  }

  def activities = Action {
    Ok(views.html.workspace.activities())
  }

  def newProjectDialog() = Action {
    Ok(views.html.workspace.newProjectDialog())
  }

  def importProjectDialog() = Action {
    Ok(views.html.workspace.importProjectDialog())
  }

  def importLinkSpecDialog(project: String) = Action {
    Ok(views.html.workspace.importLinkSpecDialog(project))
  }

  def prefixDialog(project: String) = Action {
    val prefixes = User().workspace.project(project).config.prefixes

    Ok(views.html.workspace.prefixDialog(project, prefixes, PrefixRegistry.all))
  }

  def resourcesDialog(project: String) = Action {
    val resourceManager = User().workspace.project(project).resources

    Ok(views.html.workspace.resourcesDialog(project, resourceManager))
  }

  def importExample(project: String) = Action {
    val workspaceProvider = User().workspace.provider
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").load
    workspaceProvider.importProjectMarshaled(Identifier(project), inputStream, XmlZipProjectMarshaling())

    User().workspace.reload()

    Ok
  }

  def executeProjectDialog(projectName: String) = Action {
    Ok(views.html.workspace.executeProjectDialog(projectName))
  }

  def projectActivityConfigDialog(projectName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val activity = project.activity(activityName)
    Ok(views.html.workspace.activity.projectActivityConfigDialog(activity))
  }

  def taskActivityConfigDialog(projectName: String, taskName: String, activityName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val activity = task.activity(activityName)
    Ok(views.html.workspace.activity.taskActivityConfigDialog(activity))
  }
}