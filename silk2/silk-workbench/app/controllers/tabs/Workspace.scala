package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.util.PrefixRegistry

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

  def sourceDialog(project: String, source: String) = Action {
    Ok(views.html.workspace.sourceDialog(project, source))
  }

  def linkingTaskDialog(project: String, task: String) = Action {
    Ok(views.html.workspace.linkingTaskDialog(project, task))
  }

  def outputDialog(project: String, output: String) = Action {
    Ok(views.html.workspace.outputDialog(project, output))
  }
}