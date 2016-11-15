package controllers.workspace

import config.WorkbenchConfig
import org.silkframework.util.Identifier
import org.silkframework.workspace.xml.XmlZipProjectMarshaling
import org.silkframework.workspace.{PrefixRegistry, User}
import play.api.mvc.{Action, AnyContent, Controller}

class Workspace extends Controller {

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.workspace.workspace())
  }

  def tree: Action[AnyContent] = Action {
    Ok(views.html.workspace.workspaceTree(User().workspace))
  }

  def activities: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.workspace.activities())
  }

  def newProjectDialog(): Action[AnyContent] = Action {
    Ok(views.html.workspace.newProjectDialog())
  }

  def importProjectDialog(): Action[AnyContent] = Action {
    Ok(views.html.workspace.importProjectDialog())
  }

  def removeTaskDialog(name: String, path: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.removeTaskDialog(name, path))
  }

  def removeResourceDialog(name: String, path: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.removeResourceDialog(name, path))
  }

  def importLinkSpecDialog(project: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.importLinkSpecDialog(project))
  }

  def prefixDialog(project: String): Action[AnyContent] = Action {
    val prefixes = User().workspace.project(project).config.prefixes

    Ok(views.html.workspace.prefixDialog(project, prefixes, PrefixRegistry.all))
  }

  def resourcesDialog(project: String): Action[AnyContent] = Action {
    val resourceManager = User().workspace.project(project).resources

    Ok(views.html.workspace.resourcesDialog(project, resourceManager))
  }

  def importExample(project: String): Action[AnyContent] = Action {
    val workspace = User().workspace
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").load
    workspace.importProject(Identifier(project), inputStream, XmlZipProjectMarshaling())

    User().workspace.reload()

    Ok
  }

  def executeProjectDialog(projectName: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.executeProjectDialog(projectName))
  }

  def projectActivityConfigDialog(projectName: String, activityName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val activity = project.activity(activityName)
    Ok(views.html.workspace.activity.projectActivityConfigDialog(activity))
  }

  def taskActivityConfigDialog(projectName: String, taskName: String, activityName: String): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val activity = task.activity(activityName)
    Ok(views.html.workspace.activity.taskActivityConfigDialog(activity))
  }
}