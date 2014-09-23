package controllers.workspace

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.dataset.rdf.{ResultSet, RdfDatasetPlugin}
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, EntityDescription}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import play.api.mvc.{Action, Controller}
import plugins.Context

object Datasets extends Controller {

  def getDataset(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[DatasetTask](sourceName)
    val sourceXml = task.dataset.toXML

    Ok(sourceXml)
  }

  def putDataset(projectName: String, sourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    request.body.asXml match {
      case Some(xml) =>
        try {
          val sourceTask = DatasetTask(project, Dataset.fromXML(xml.head, project.resources))
          project.updateTask(sourceTask)
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  def deleteDataset(project: String, source: String) = Action {
    User().workspace.project(project).removeTask[DatasetTask](source)
    Ok
  }

  def view(project: String, task: String, maxEntities: Int) = Action { request =>
    val context = Context.get[DatasetTask](project, task, request.path)
    val source = context.task.source

    val paths = source.retrievePaths().map(_._1).toIndexedSeq
    val entityDesc = EntityDescription("a", SparqlRestriction.empty, paths)
    val entities = source.retrieve(entityDesc).take(maxEntities).toList

    Ok(views.html.workspace.dataset(paths, entities))
  }

  def sparql(project: String, task: String, query: String = "") = Action { request =>
    val context = Context.get[DatasetTask](project, task, request.path)
    val prefixes = context.project.config.prefixes

    context.task.dataset.plugin match {
      case rdf: RdfDatasetPlugin =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        var queryResults: Option[ResultSet] = None
        if(!query.isEmpty) {
          queryResults = Some(sparqlEndpoint.query(query))
        }
        Ok(views.html.workspace.sparql(sparqlEndpoint, prefixes, queryResults))
      case _ => BadRequest("This is not an RDF-Dataset.")
    }
  }

}