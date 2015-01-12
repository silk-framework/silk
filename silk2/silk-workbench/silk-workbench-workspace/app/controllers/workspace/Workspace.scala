package controllers.workspace

import config.WorkbenchConfig
import de.fuberlin.wiwiss.silk.entity.Restriction.{Condition, Operator, Or}
import de.fuberlin.wiwiss.silk.entity.{ForwardOperator, Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.ValidationException
import de.fuberlin.wiwiss.silk.util.convert.SparqlRestrictionParser
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.{Constants, PrefixRegistry, User}
import play.Logger
import play.api.mvc.{Action, Controller}

object Workspace extends Controller {

  def index = Action {
    Ok(views.html.workspace.workspace())
  }

  def tree = Action {
    Ok(views.html.workspace.workspaceTree(User().workspace))
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

  def restrictionDialog(projectName: String, sourceName: String, sourceOrTarget: String, restriction: String) = Action {
    val project = User().workspace.project(projectName)
    val pathCache = project.task[DatasetTask](sourceName).cache
    implicit val prefixes = project.config.prefixes

    val variable = sourceOrTarget match {
      case "source" => Constants.SourceVariable
      case "target" => Constants.TargetVariable
    }

    // Try to parse the SPARQL restriction
    val restrictionParser = new SparqlRestrictionParser()
    val restrictionTree =
      try {
        restrictionParser(SparqlRestriction.fromSparql(variable, restriction))
      } catch {
        case ex: ValidationException =>
          Logger.info(s"Could not parse SPARQL restriction '$restriction'.")
          Restriction.empty
      }

    // Collect all type statements from the restriction
    def collectTypes(op: Operator): Set[String] = op match {
      case Condition(path, value) => path.operators match {
        case ForwardOperator(uri) :: Nil if uri.uri == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" => Set(value)
        case _ => Set.empty
      }
      case Or(ops) => ops.toSet.flatMap(collectTypes)
      case _ => Set.empty
    }
    val types = restrictionTree.operator match {
      case Some(op) => collectTypes(op)
      case None => Set.empty[String]
    }

    Ok(views.html.workspace.restrictionDialog(project, restriction, types, pathCache))
  }

  def importExample(project: String) = Action {
    val inputStream = WorkbenchConfig.getResourceLoader.get("example.zip").load
    User().workspace.importProject(project, inputStream)
    inputStream.close()

    Ok
  }
}