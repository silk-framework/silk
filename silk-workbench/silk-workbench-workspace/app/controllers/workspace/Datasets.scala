package controllers.workspace

import org.silkframework.dataset.rdf.{RdfDatasetPlugin, SparqlResults}
import org.silkframework.dataset.{DatasetPluginAutoConfigurable, Dataset, DatasetPlugin}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.dataset.TypesCache
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc.{Action, Controller}
import plugins.Context

object Datasets extends Controller {

  def getDataset(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[Dataset](sourceName)
    val sourceXml = Serialization.toXml(task.data)

    Ok(sourceXml)
  }

  def getDatasetAutoConfigured(projectName: String, sourceName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[Dataset](sourceName)
    val datasetPlugin = task.data.plugin
    datasetPlugin match {
      case autoConfigurable: DatasetPlugin with DatasetPluginAutoConfigurable[_] =>
        val autoConfDataset = task.data.copy(plugin = autoConfigurable.autoConfiguredDatasetPlugin)
        val sourceXml = Serialization.toXml(autoConfDataset)

        Ok(sourceXml)
      case _ =>
        NotImplemented("The dataset type does not support auto-configuration.")
    }
  }

  def putDataset(projectName: String, sourceName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val resources = project.resources
    request.body.asXml match {
      case Some(xml) =>
        try {
          val dataset = Serialization.fromXml[Dataset](xml.head)
          project.updateTask(dataset.id, dataset)
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("Expecting dataset in request body as text/xml.")
    }
  }}

  def deleteDataset(project: String, source: String) = Action {
    User().workspace.project(project).removeTask[Dataset](source)
    Ok
  }

  def datasetDialog(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val task = if(taskName.isEmpty) None else project.taskOption[Dataset](taskName).map(_.data)
    Ok(views.html.workspace.dataset.datasetDialog(project, taskName, task))
  }

  def datasetDialogAutoConfigured(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    implicit val resources = project.resources
    request.body.asXml match {
      case Some(xml) =>
        try {
          val dataset = Serialization.fromXml[Dataset](xml.head)
          Ok(views.html.workspace.dataset.datasetDialog(project, taskName, Some(dataset)))
          Ok
        } catch {
          case ex: Exception => BadRequest(ex.getMessage)
        }
      case None => BadRequest("Expecting dataset in request body as text/xml.")
    }
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
    val entityDesc = EntitySchema(firstTypes, paths)
    val entities = source.retrieve(entityDesc).take(maxEntities).toList

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

  def types(project: String, task: String, search: String = "") = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val types = context.task.activity[TypesCache].value.types
    val filteredTypes = types.filter(_.contains(search))

    Ok(JsArray(filteredTypes.map(JsString)))
  }

}