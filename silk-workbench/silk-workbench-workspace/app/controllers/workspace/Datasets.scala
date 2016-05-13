package controllers.workspace

import models.JsonError
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
      case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
        val autoConfDataset = task.data.copy(plugin = autoConfigurable.autoConfigured)
        val sourceXml = Serialization.toXml(autoConfDataset)

        Ok(sourceXml)
      case _ =>
        NotImplemented(JsonError("The dataset type does not support auto-configuration."))
    }
  }

  def putDataset(projectName: String, sourceName: String, autoConfigure: Boolean) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    implicit val resources = project.resources
    request.body.asXml match {
      case Some(xml) =>
        try {
          val dataset = Serialization.fromXml[Dataset](xml.head)
          if(autoConfigure) {
            dataset.plugin match {
              case autoConfigurable: DatasetPluginAutoConfigurable[_] =>
                project.updateTask(dataset.id, dataset.copy(plugin = autoConfigurable.autoConfigured))
                Ok
              case _ =>
                NotImplemented(JsonError("The dataset type does not support auto-configuration."))
            }
          } else {
            project.updateTask(dataset.id, dataset)
            Ok
          }
        } catch {
          case ex: Exception => BadRequest(JsonError(ex))
        }
      case None => BadRequest(JsonError("Expecting dataset in request body as text/xml."))
    }
  }}

  def deleteDataset(project: String, source: String) = Action {
    User().workspace.project(project).removeTask[Dataset](source)
    Ok
  }

  def datasetDialog(projectName: String, datasetName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val datasetPlugin = if(datasetName.isEmpty) None else project.taskOption[Dataset](datasetName).map(_.data.plugin)
    Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, datasetPlugin))
  }

  def datasetDialogAutoConfigured(projectName: String, datasetName: String, pluginId: String) = Action { request =>
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    val datasetParams = request.queryString.mapValues(_.head)
    val datasetPlugin = DatasetPlugin.apply(pluginId, datasetParams)
    datasetPlugin match {
      case ds: DatasetPluginAutoConfigurable[_] =>
        Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, Some(ds.autoConfigured)))
      case _ =>
        NotImplemented("This dataset plugin does not support auto-configuration.")
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

  /** Get types of a dataset including the search string */
  def types(project: String, task: String, search: String = "") = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val prefixes = context.project.config.prefixes

    val typesFull = context.task.activity[TypesCache].value.types
    val typesResolved = typesFull.map(prefixes.shorten)
    val allTypes = (typesResolved ++ typesFull).distinct
    val filteredTypes = allTypes.filter(_.contains(search))

    Ok(JsArray(filteredTypes.map(JsString)))
  }

  /** Get all types of the dataset */
  def getDatasetTypes(project: String, task: String) = Action { request =>
    val context = Context.get[Dataset](project, task, request.path)
    val types = context.task.activity[TypesCache].value.types

    Ok(JsArray(types.map(JsString)))
  }
}