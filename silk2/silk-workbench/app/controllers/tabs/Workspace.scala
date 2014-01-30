package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.util.PrefixRegistry
import models.WorkbenchConfig
import java.io.FileInputStream

object Workspace extends Controller {

  def index = Action {
    Ok(views.html.workspace.workspace())
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
    val resourceManager = User().workspace.project(project).resourceManager

    Ok(views.html.workspace.resourcesDialog(project, resourceManager))
  }

  def sourceDialog(project: String, source: String) = Action {
    Ok(views.html.workspace.sourceDialog(project, source))
  }

  def linkingTaskDialog(project: String, task: String) = Action {
    Ok(views.html.workspace.linkingTaskDialog(project, task))
  }

  def restrictionDialog(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val pathCache = project.sourceModule.task(sourceName).cache

    Ok(views.html.workspace.restrictionDialog(project, pathCache))
  }

  def outputDialog(project: String, output: String) = Action {
    Ok(views.html.workspace.outputDialog(project, output))
  }

  def importExample(project: String) = Action {
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").load
    User().workspace.importProject(project, inputStream)
    inputStream.close()

    Ok
  }
}