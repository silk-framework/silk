package controllers.workspace

import java.nio.file.{Files, StandardCopyOption}

import config.WorkbenchConfig
import controllers.core.{RequestUserContextAction, UserContextAction}
import javax.inject.Inject
import org.silkframework.util.Identifier
import org.silkframework.workspace.xml.XmlZipProjectMarshaling
import org.silkframework.workspace.{PrefixRegistry, WorkspaceFactory}
import play.api.mvc.{InjectedController, Action, AnyContent, ControllerComponents}

class WorkspaceController @Inject() () extends InjectedController{

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.workspace.workspace())
  }

  def tree: Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workspace.workspaceTree(WorkspaceFactory().workspace))
  }

  def activities: Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    Ok(views.html.workspace.activities())
  }

  def newProjectDialog(): Action[AnyContent] = Action {
    Ok(views.html.workspace.newProjectDialog())
  }

  def importProjectDialog(): Action[AnyContent] = Action {
    Ok(views.html.workspace.importProjectDialog())
  }

  def removeProjectDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(views.html.workspace.removeProjectDialog(project, WorkspaceFactory().workspace.repository.sharedResources))
  }

  def removeTaskDialog(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val dependentTasks = task.findDependentTasks(recursive = false).map(project.anyTask(_).taskLabel()).toSeq

    Ok(views.html.workspace.removeTaskDialog(projectName, taskName, task.taskLabel(), dependentTasks))
  }

  def removeResourceDialog(name: String, path: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.removeResourceDialog(name, path))
  }

  def importLinkSpecDialog(project: String): Action[AnyContent] = Action {
    Ok(views.html.workspace.importLinkSpecDialog(project))
  }

  def prefixDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val prefixes = WorkspaceFactory().workspace.project(project).config.prefixes

    Ok(views.html.workspace.prefixDialog(project, prefixes, PrefixRegistry.all))
  }

  def resourcesDialog(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val resourceManager = WorkspaceFactory().workspace.project(project).resources

    Ok(views.html.workspace.resourcesDialog(project, resourceManager))
  }

  def importExample(project: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val workspace = WorkspaceFactory().workspace

    // Load example and write it to a temporary file
    val exampleFile = Files.createTempFile("example", ".zip")
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").inputStream

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

  def executeProjectDialog(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    Ok(views.html.workspace.executeProjectDialog(project))
  }

  def projectActivityConfigDialog(projectName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val activity = project.activity(activityName)
    Ok(views.html.workspace.activity.projectActivityConfigDialog(activity))
  }

  def taskActivityConfigDialog(projectName: String, taskName: String, activityName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.anyTask(taskName)
    val activity = task.activity(activityName)
    Ok(views.html.workspace.activity.taskActivityConfigDialog(activity))
  }

  def cloneProjectDialog(project: String) = Action {
    Ok(views.html.workspace.cloneProjectDialog(project))
  }

  def cloneTaskDialog(project: String, task: String) = Action {
    Ok(views.html.workspace.cloneTaskDialog(project, task))
  }
}