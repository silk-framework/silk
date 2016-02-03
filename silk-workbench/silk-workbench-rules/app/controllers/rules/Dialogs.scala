package controllers.rules

import controllers.workspace.Workspace._
import org.silkframework.dataset.Dataset
import org.silkframework.entity.{ForwardOperator, Restriction}
import org.silkframework.entity.Restriction.{Or, Condition, Operator}
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlRestrictionParser}
import org.silkframework.runtime.serialization.ValidationException
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.dataset.TypesCache
import play.Logger
import play.api.mvc.{Action, Controller}

object Dialogs extends Controller {

  def restrictionDialog(projectName: String, sourceName: String, varName: String, restriction: String) = Action {
    val project = User().workspace.project(projectName)
    val typesCache = project.task[Dataset](sourceName).activity[TypesCache].value.typesByFrequency
    implicit val prefixes = project.config.prefixes

    // Try to parse the SPARQL restriction
    val restrictionParser = new SparqlRestrictionParser()
    val restrictionTree =
      try {
        restrictionParser(SparqlRestriction.fromSparql(varName, restriction))
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

    Ok(views.html.dialogs.restrictionDialog(project, varName, restriction, types, typesCache))
  }

}
