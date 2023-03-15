package controllers.workspaceApi.coreApi

import config.WorkbenchLinks
import controllers.core.UserContextActions
import controllers.util.{DatasetUtils, PluginUsageCollector, TextSearchUtils}
import controllers.workspaceApi.coreApi.doc.PluginApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetPluginAutoConfigurable, DatasetSpec}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.{Aggregator, DistanceMeasure}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.{PluginDescription, PluginList, PluginRegistry}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.PluginDescriptionSerializers.PluginListJsonFormat
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json._
import play.api.mvc._

import javax.inject.Inject
import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Workspace task plugin related endpoints.
  */
@Tag(name = "Plugins", description = "Provides information about all installed plugins.")
class PluginApi @Inject()() extends InjectedController with UserContextActions {

  /** All plugins that can be created in the workspace. */
  @Operation(
    summary = "Task plugins",
    description = "A list of plugins that can be created as workspace tasks, e.g. datasets, transform tasks etc. The result of this endpoint only contains meta data of the plugin, i.e. title, description and categories. To fetch the schema details of a specific plugin use the /plugin endpoint.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApiDoc.taskPluginsExampleJson))
        ))
      )
    ))
  def taskPlugins(@Parameter(
    name = "addMarkdownDocumentation",
    description = "Add markdown documentation to the result.",
    required = false,
    in = ParameterIn.QUERY,
    schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
  )
                  addMarkdownDocumentation: Boolean,
                  @Parameter(
                    name = "textQuery",
                    description = "An optional (multi word) text query to filter the list of plugins.",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String], example = "csv")
                  )
                  textQuery: Option[String],
                  @Parameter(
                    name = "category",
                    description = "An optional category. This will only return plugins from the same category.",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String], example = "file")
                  )
                  category: Option[String]): Action[AnyContent] = Action { implicit request =>
    val pluginTypes = Seq(
      "org.silkframework.dataset.Dataset",
      "org.silkframework.config.CustomTask"
    )
    val singlePlugins = Seq(
      "workflow",
      "transform",
      "linking"
    ) flatMap (pl => PluginRegistry.pluginDescriptionsById(pl, Some(Seq(classOf[TaskSpec]))))
    val singlePluginsList = PluginList(ListMap("singleTasks" -> singlePlugins), addMarkdownDocumentation, overviewOnly = true)
    pluginResult(addMarkdownDocumentation, pluginTypes, Some(singlePluginsList), textQuery, category, overviewOnly = true)
  }

  /** Return plugin description of a single plugin. */
  @Operation(
    summary = "Plugin description",
    description = "The plugin description of a specific plugin, including meta data and JSON schema.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = PluginApiDoc.pluginJsonDescription,
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApiDoc.pluginDescriptionExampleJson))
        ))
      )
    ))
  def plugin(@Parameter(
               name = "pluginId",
               description = "The plugin identifier.",
               required = true,
               in = ParameterIn.PATH,
               schema = new Schema(implementation = classOf[String], example = "csv")
             )
             pluginId: String,
             @Parameter(
               name = "addMarkdownDocumentation",
               description = "Add markdown documentation to the result.",
               required = false,
               in = ParameterIn.QUERY,
               schema = new Schema(implementation = classOf[Boolean], example = "false")
             )
             addMarkdownDocumentation: Boolean,
             @Parameter(
               name = "pretty",
               description = "If true, JSON output will be pretty printed.",
               required = false,
               in = ParameterIn.QUERY,
               schema = new Schema(implementation = classOf[Boolean], example = "true")
             )
             pretty: Boolean,
             @Parameter(
               name = "withLabels",
               description = "If true, all plugin parameter values will be reified in a new object that has an optional label property. A label is added for all auto-completable parameters that have the 'autoCompleteValueWithLabels' property set to true. This guarantees that a user always sees the label of such values.",
               required = false,
               in = ParameterIn.QUERY,
               schema = new Schema(implementation = classOf[Boolean])
             )
             withLabels: Boolean): Action[AnyContent] = Action { implicit request =>
    PluginRegistry.pluginDescriptionsById(pluginId, Some(Seq(classOf[TaskSpec], classOf[Dataset]))).headOption match {
      case Some(pluginDesc) =>
        implicit val writeContext: WriteContext[JsValue] = WriteContext.empty[JsValue]
        var resultJson = PluginListJsonFormat.serializePlugin(pluginDesc, addMarkdownDocumentation, overviewOnly = false,
          taskType = PluginApiCache.taskTypeByClass(pluginDesc.pluginClass), withLabels = withLabels)
        val autoConfigurable = classOf[DatasetPluginAutoConfigurable[_]].isAssignableFrom(pluginDesc.pluginClass)
        resultJson += ("autoConfigurable" -> JsBoolean(autoConfigurable))
        result(pretty, resultJson)
      case None =>
        NotFound
    }
  }

  @Operation(
    summary = "Plugin usages",
    description = "Returns a list of all usages of a given plugin. Currently lists usages in projects as tasks and as within linking and transform rules.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApiDoc.pluginUsagesExample))
        ))
      )
    ))
  def pluginUsages(@Parameter(
                     name = "pluginId",
                     description = "The plugin identifier.",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String], example = "csv")
                   )
                   pluginId: String): Action[AnyContent] = RequestUserContextAction { request =>implicit userContext =>
    val usages = mutable.Buffer[JsObject]()

    for(project <- WorkspaceFactory().workspace.projects) {
      for(task <- project.allTasks) {
        val pluginIds = PluginUsageCollector.pluginUsages(task.data)
        if(pluginIds.contains(pluginId)) {
          usages += Json.obj(
            "project" -> project.id.toString,
            "task" -> task.id.toString,
            "link" -> WorkbenchLinks.editorLink(task)
          )
        }
      }
    }

    Ok(JsArray(usages))
  }

  lazy val resourceBasedDatasetPluginIds: Seq[JsString] = DatasetUtils.resourceBasedDatasetPluginIds
    .map(s => JsString(s))

  @Operation(
    summary = "Resource based dataset plugin IDs",
    description = "Returns a list of plugin IDs of all resource based datasets.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApiDoc.resourceBasedPluginIdsExample))
        ))
      )
    ))
  def resourceBasedDatasetIds(): Action[AnyContent] = UserContextAction { implicit userContext =>
    Ok(JsArray(resourceBasedDatasetPluginIds))
  }

  /** Rule (operator) plugins. */
  @Operation(
    summary = "Rule operator plugins",
    description = "A list of plugins that can be used in rule editors. Contains meta data of the plugin, i.e. title, description and categories and parameter information.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApiDoc.operatorPluginsExampleJson))
        ))
      )
    ))
  def ruleOperatorPlugins(@Parameter(
                            name = "addMarkdownDocumentation",
                            description = "Add markdown documentation to the result.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                          )
                          addMarkdownDocumentation: Boolean,
                          @Parameter(
                            name = "textQuery",
                            description = "An optional (multi word) text query to filter the list of plugins.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String], example = "csv")
                          )
                          textQuery: Option[String],
                          @Parameter(
                            name = "category",
                            description = "An optional category. This will only return plugins from the same category.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[String], example = "file")
                          )
                          category: Option[String],
                          @Parameter(
                            name = "overviewOnly",
                            description = "If false the whole plugin specification will be returned, else only the high-level meta data is returned, e.g. label and category.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                          )
                          overviewOnly: Option[Boolean],
                          @Parameter(
                            name = "inputOperatorsOnly",
                            description = "If set to true then only input rule operators will be returned, i.e. transformation operators.",
                            required = false,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Boolean]))
                          inputOperatorsOnly: Option[Boolean]): Action[AnyContent] = Action { implicit request =>
    val inputOperatorBase = Seq(
      "org.silkframework.rule.input.Transformer"
    )
    val linkingOperatorsBase = Seq(
      "org.silkframework.rule.similarity.DistanceMeasure",
      "org.silkframework.rule.similarity.Aggregator"
    )
    val pluginTypes = if(inputOperatorsOnly.getOrElse(false)) inputOperatorBase else inputOperatorBase ++ linkingOperatorsBase
    pluginResult(addMarkdownDocumentation, pluginTypes, None, textQuery, category, overviewOnly = overviewOnly.getOrElse(false))
  }

  private def result(pretty: Boolean, resultJson: JsValue): Result = {
    if (pretty) {
      Ok(Json.prettyPrint(resultJson)).as(JSON)
    } else {
      Ok(resultJson)
    }
  }

  private def pluginResult(addMarkdownDocumentation: Boolean,
                           pluginTypes: Seq[String],
                           singlePluginList: Option[PluginList],
                           textQuery: Option[String],
                           category: Option[String],
                           overviewOnly: Boolean)
                          (implicit request: Request[AnyContent]): Result = {
    def filter(pluginDescription: PluginDescription[_]): Boolean = {
      val matchesCategory = category.forall(c => pluginDescription.categories.contains(c))
      lazy val matchesTextQuery = textQuery forall { query =>
        val queryTerms = TextSearchUtils.extractSearchTerms(query)
        val overallText = pluginDescription.label + " " + pluginDescription.description
        TextSearchUtils.matchesSearchTerm(queryTerms, overallText)
      }
      matchesCategory && matchesTextQuery
    }

    val pluginList = PluginList.load(pluginTypes, addMarkdownDocumentation, overviewOnly = overviewOnly)
    val allPlugins = pluginList.pluginsByType ++ singlePluginList.map(_.pluginsByType).getOrElse(ListMap.empty)
    val filteredPlugins = allPlugins map { case (key, pds) =>
      val filteredPDs = pds.filter(pd => filter(pd))
      (key, filteredPDs)
    }
    implicit val writeContext: WriteContext[JsValue] = WriteContext.empty[JsValue]
    val pluginListJson = JsonSerializers.toJson(pluginList.copy(pluginsByType = filteredPlugins))
    val pluginJsonWithTaskAndPluginType = pluginListJson.as[JsObject].fields.map { case (pluginId, pluginJson) =>
      val withTaskType = PluginApiCache.taskType(pluginId) match {
        case Some(taskType) => pluginJson.as[JsObject] + (JsonSerializers.TASKTYPE -> JsString(taskType))
        case None => pluginJson
      }
      val withPluginType = PluginApiCache.pluginType(pluginId) match {
        case Some(pluginType) => withTaskType.as[JsObject] + (JsonSerializers.PLUGIN_TYPE -> JsString(pluginType))
        case None => withTaskType
      }
      (pluginId, withPluginType)
    }
    Ok(JsObject(pluginJsonWithTaskAndPluginType))
  }
}


object PluginApiCache {

  @volatile
  private var pluginRegistryTimestamp = Long.MinValue

  @volatile
  private var itemTypeMapById: Map[String, String] = Map.empty

  @volatile
  private var pluginTypeMapById: Map[String, String] = Map.empty

  def taskType(pluginId: String): Option[String] = {
    updateCache()
    itemTypeMapById.get(pluginId)
  }

  def pluginType(pluginId: String): Option[String] = {
    updateCache()
    pluginTypeMapById.get(pluginId)
  }

  private val taskTypes = Seq(
    JsonSerializers.TASK_TYPE_DATASET -> classOf[Dataset],
    JsonSerializers.TASK_TYPE_DATASET -> classOf[DatasetSpec[_ <: Dataset]],
    JsonSerializers.TASK_TYPE_CUSTOM_TASK -> classOf[CustomTask],
    JsonSerializers.TASK_TYPE_WORKFLOW -> classOf[Workflow],
    JsonSerializers.TASK_TYPE_TRANSFORM -> classOf[TransformSpec],
    JsonSerializers.TASK_TYPE_LINKING -> classOf[LinkSpec]
  )

  private val ruleOperatorTypes = Seq(
    JsonSerializers.TRANSFORM_OPERATOR -> classOf[Transformer],
    JsonSerializers.AGGREGATION_OPERATOR -> classOf[Aggregator],
    JsonSerializers.COMPARISON_OPERATOR -> classOf[DistanceMeasure]
  )

  private val allPluginTypes = taskTypes ++ ruleOperatorTypes

  def pluginTypeByClass(pluginClass: Class[_]): Option[String] = {
    allPluginTypes.find(_._2.isAssignableFrom(pluginClass)).map(_._1)
  }

  def taskTypeByClass(pluginClass: Class[_]): Option[String] = {
    taskTypes.find(_._2.isAssignableFrom(pluginClass)).map(_._1)
  }

  private def updateCache(): Unit = {
    if(PluginRegistry.lastUpdateTimestamp > pluginRegistryTimestamp) {
      pluginRegistryTimestamp = PluginRegistry.lastUpdateTimestamp
      itemTypeMapById = {
        PluginRegistry.allPlugins
          .filter(pd => classOf[TaskSpec].isAssignableFrom(pd.pluginClass) || classOf[Dataset].isAssignableFrom(pd.pluginClass))
          .flatMap(pd => taskTypeByClass(pd.pluginClass).map(taskType => (pd.id.toString, taskType)))
          .toMap
      }
      pluginTypeMapById = {
        PluginRegistry.allPlugins
          .filter(pd => Seq(classOf[TaskSpec], classOf[Dataset], classOf[Transformer], classOf[Aggregator], classOf[DistanceMeasure])
            .exists(_.isAssignableFrom(pd.pluginClass)))
          .flatMap(pd => pluginTypeByClass(pd.pluginClass).map(pluginType => (pd.id.toString, pluginType)))
          .toMap
      }
    }
  }
}
