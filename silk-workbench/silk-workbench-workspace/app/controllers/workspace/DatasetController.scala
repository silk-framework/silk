package controllers.workspace

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetPluginAutoConfigurable, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlResults}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.WorkspaceFactory
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class DatasetController @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  def datasetDialog(projectName: String,
                    datasetName: String,
                    title: String = "Edit Dataset",
                    createDialog: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val datasetPlugin = if (datasetName.isEmpty) None else project.taskOption[GenericDatasetSpec](datasetName).map(_.data)
    Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, datasetPlugin, title, createDialog))
  }

  def datasetDialogAutoConfigured(projectName: String, datasetName: String, pluginId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val createDialog = project.taskOption[DatasetSpec[Dataset]](datasetName).isEmpty
    val dialogTitle = if(createDialog) "Create Dataset" else "Edit Dataset"
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    val datasetParams = request.queryString.mapValues(_.head)
    val datasetPlugin = Dataset.apply(pluginId, datasetParams)
    datasetPlugin match {
      case ds: DatasetPluginAutoConfigurable[_] =>
        Ok(views.html.workspace.dataset.datasetDialog(project, datasetName, Some(DatasetSpec(ds.autoConfigured)), title = dialogTitle, createDialog = createDialog))
      case _ =>
        ErrorResult(BadUserInputException("This dataset type does not support auto-configuration."))
    }
  }

  def dataset(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    context.task.data match {
      case dataset: GenericDatasetSpec =>
        if (dataset.plugin.isInstanceOf[RdfDataset]) {
          Redirect(routes.DatasetController.sparql(project, task))
        } else {
          Redirect(routes.DatasetController.table(project, task))
        }
    }
  }

  def table(project: String, task: String, maxEntities: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    val source = context.task.data.source
    implicit val prefixes: Prefixes = context.project.config.prefixes

    val firstTypes = source.retrieveTypes().head._1
    val paths = source.retrievePaths(firstTypes).toIndexedSeq
    val entityDesc = EntitySchema(firstTypes, paths)
    val entities = source.retrieve(entityDesc).entities.take(maxEntities).toList

    Ok(views.html.workspace.dataset.table(context, paths.map(_.toUntypedPath), entities))
  }

  def sparql(project: String, task: String, query: String = ""): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)

    context.task.data.plugin match {
      case rdf: RdfDataset =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        var queryResults: Option[SparqlResults] = None
        if (!query.isEmpty) {
          queryResults = Some(sparqlEndpoint.select(query))
        }
        Ok(views.html.workspace.dataset.sparql(context, sparqlEndpoint, query, queryResults))
      case _ =>
        ErrorResult(BadUserInputException("This is not an RDF-Dataset."))
    }
  }

}
