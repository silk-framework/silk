package controllers.workspace

import config.WorkbenchConfig
import controllers.workspace.WorkspaceApi._
import org.silkframework.dataset.Dataset
import org.silkframework.entity.Restriction.{Condition, Operator, Or}
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlRestrictionParser}
import org.silkframework.entity.{ForwardOperator, Restriction}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.serialization.ValidationException
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.activity.dataset.TypesCache
import org.silkframework.workspace.xml.XmlWorkspaceProvider
import org.silkframework.workspace.{PrefixRegistry, User}
import play.Logger
import play.api.mvc.{Action, Controller}

object Workspace extends Controller {

  def index = Action {
    Ok(views.html.workspace.workspace())
  }

  def tree = Action {
    Ok(views.html.workspace.workspaceTree(User().workspace))
  }

  def status = Action {
    Ok(views.html.workspace.status())
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
    // Import example into an XML workspace
    val xmlWorkspace = new XmlWorkspaceProvider(new InMemoryResourceManager())
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").load
    xmlWorkspace.importProject(project, inputStream)
    inputStream.close()
    // Transfer into current workspace
    WorkspaceIO.copyProjects(xmlWorkspace, User().workspace.provider)
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