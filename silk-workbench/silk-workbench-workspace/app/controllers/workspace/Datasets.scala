package controllers.workspace

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.dataset.rdf.{RdfDatasetPlugin, SparqlResults}
import de.fuberlin.wiwiss.silk.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization
import de.fuberlin.wiwiss.silk.workspace.User
import play.api.mvc.{Action, Controller}
import plugins.Context

object Datasets extends Controller {

  def datasetDialog(project: String, task: String) = Action {
    Ok(views.html.workspace.dataset.datasetDialog(project, task))
  }

  def getDataset(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[Dataset](sourceName)
    val sourceXml = Serialization.toXml(task.data)

    Ok(sourceXml)
  }

  def putDataset(projectName: String, sourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val resources = project.resources
    request.body.asXml match {
      case Some(xml) =>
        try {
          val sourceTask = Serialization.fromXml[Dataset](xml.head)
          project.updateTask(sourceTask.id, sourceTask)
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  def deleteDataset(project: String, source: String) = Action {
    User().workspace.project(project).removeTask[Dataset](source)
    Ok
  }

  def dataset(project: String, task: String) = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    Ok(views.html.workspace.dataset.dataset(context))
  }

  def table(project: String, task: String, maxEntities: Int) = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val source = context.task.data.source

    val firstTypes = source.retrieveTypes().head._1
    val paths = source.retrievePaths(firstTypes).toIndexedSeq
    val entityDesc = SparqlEntitySchema("a", SparqlRestriction.empty, paths)
    val entities = source.retrieveSparqlEntities(entityDesc).take(maxEntities).toList

    Ok(views.html.workspace.dataset.table(context, paths, entities))
  }

  def sparql(project: String, task: String, query: String = "") = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)

    context.task.data.plugin match {
      case rdf: RdfDatasetPlugin =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        var queryResults: Option[SparqlResults] = None
        if(!query.isEmpty) {
          queryResults = Some(sparqlEndpoint.select(query))
        }
        Ok(views.html.workspace.dataset.sparql(context, sparqlEndpoint, query, queryResults))
      case _ => BadRequest("This is not an RDF-Dataset.")
    }
  }

}