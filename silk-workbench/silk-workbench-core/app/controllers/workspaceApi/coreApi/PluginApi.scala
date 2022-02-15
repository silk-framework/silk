package controllers.workspaceApi.coreApi

import config.WorkbenchLinks
import controllers.core.UserContextActions
import controllers.util.{PluginUsageCollector, TextSearchUtils}
import controllers.workspaceApi.coreApi.doc.PluginApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.{PluginDescription, PluginList, PluginRegistry}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.PluginSerializers.PluginListJsonFormat
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
class PluginApi @Inject()(pluginCache: PluginApiCache) extends InjectedController with UserContextActions {

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
    pluginResult(addMarkdownDocumentation, pluginTypes, singlePluginsList, textQuery, category)
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
             pretty: Boolean): Action[AnyContent] = Action { implicit request =>
    PluginRegistry.pluginDescriptionsById(pluginId, Some(Seq(classOf[TaskSpec], classOf[Dataset]))).headOption match {
      case Some(pluginDesc) =>
        implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
        val resultJson = PluginListJsonFormat.serializePlugin(pluginDesc, addMarkdownDocumentation, overviewOnly = false,
          taskType = pluginCache.taskTypeByClass(pluginDesc.pluginClass))
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

  private def result(pretty: Boolean, resultJson: JsValue): Result = {
    if (pretty) {
      Ok(Json.prettyPrint(resultJson)).as(JSON)
    } else {
      Ok(resultJson)
    }
  }

  private def pluginResult(addMarkdownDocumentation: Boolean,
                           pluginTypes: Seq[String],
                           singlePluginList: PluginList,
                           textQuery: Option[String],
                           category: Option[String])
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

    val pluginList = PluginList.load(pluginTypes, addMarkdownDocumentation, overviewOnly = true)
    val allPlugins = pluginList.pluginsByType ++ singlePluginList.pluginsByType
    val filteredPlugins = allPlugins map { case (key, pds) =>
      val filteredPDs = pds.filter(pd => filter(pd))
      (key, filteredPDs)
    }
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    val pluginListJson = JsonSerializers.toJson(pluginList.copy(pluginsByType = filteredPlugins))
    val pluginJsonWithTaskType = pluginListJson.as[JsObject].fields.map { case (pluginId, pluginJson) =>
      pluginCache.taskType(pluginId) match {
        case Some(taskType) => (pluginId, pluginJson.as[JsObject] + (JsonSerializers.TASKTYPE -> JsString(taskType)))
        case None => (pluginId, pluginJson)
      }
    }
    Ok(JsObject(pluginJsonWithTaskType))
  }
}



@javax.inject.Singleton
class PluginApiCache @Inject()() {
  private lazy val itemTypeMapById: Map[String, String] = {
    PluginRegistry.allPlugins
        .filter(pd => classOf[TaskSpec].isAssignableFrom(pd.pluginClass) || classOf[Dataset].isAssignableFrom(pd.pluginClass))
        .flatMap(pd => taskTypeByClass(pd.pluginClass).map(taskType => (pd.id.toString, taskType)))
        .toMap
  }

  def taskType(pluginId: String): Option[String] = {
    itemTypeMapById.get(pluginId)
  }

  def taskTypeByClass(pluginClass: Class[_]): Option[String] = {
    val taskTypes = Seq(
      JsonSerializers.TASK_TYPE_DATASET -> classOf[Dataset],
      JsonSerializers.TASK_TYPE_DATASET -> classOf[DatasetSpec[_ <: Dataset]],
      JsonSerializers.TASK_TYPE_CUSTOM_TASK -> classOf[CustomTask],
      JsonSerializers.TASK_TYPE_WORKFLOW -> classOf[Workflow],
      JsonSerializers.TASK_TYPE_TRANSFORM -> classOf[TransformSpec],
      JsonSerializers.TASK_TYPE_LINKING -> classOf[LinkSpec]
    )
    taskTypes.find(_._2.isAssignableFrom(pluginClass)).map(_._1)
  }
}
