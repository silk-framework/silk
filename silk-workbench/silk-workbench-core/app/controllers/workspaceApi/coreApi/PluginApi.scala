package controllers.workspaceApi.coreApi

import config.WorkbenchLinks
import controllers.core.UserContextActions
import controllers.util.{PluginUsageCollector, TextSearchUtils}
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject}
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
@Tag(name = "Plugin API")
class PluginApi @Inject()(pluginCache: PluginApiCache) extends InjectedController with UserContextActions {

  /** All plugins that can be created in the workspace. */
  @Operation(
    summary = "Task plugins",
    description = "A list of plugins that can be created as workspace tasks, e.g. datasets, transform tasks etc. The result of this endpoint only contains meta data of the plugin, i.e. title, description and categories. To fetch the schema details of a specific plugin use the /plugin endpoint.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApi.taskPluginsExampleJson))
        ))
      )
    ))
  def taskPlugins(@Parameter(description = "If true, MarkDown documentation will be added for plugins if available.")
                  addMarkdownDocumentation: Boolean,
                  @Parameter(description = "An optional (multi word) text query to filter the list of plugins.")
                  textQuery: Option[String],
                  @Parameter(description = "An optional category. This will only return plugins from the same category.")
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
        description = PluginApi.pluginJsonDescription,
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApi.pluginDescriptionExampleJson))
        ))
      )
    ))
  def plugin(@Parameter(description = "Plugin id")
             pluginId: String,
             @Parameter(description = "If true, MarkDown documentation will be added for plugins if available.")
             addMarkdownDocumentation: Boolean,
             @Parameter(description = "If true, JSON output will be pretty printed.")
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
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApi.pluginUsagesExample))
        ))
      )
    ))
  def pluginUsages(@Parameter(description = "Plugin id")
                   pluginId: String): Action[AnyContent] = RequestUserContextAction { request =>implicit userContext =>
    val usages = mutable.Buffer[JsObject]()

    for(project <- WorkspaceFactory().workspace.projects) {
      for(task <- project.allTasks) {
        val pluginIds = PluginUsageCollector.pluginUsages(task.data)
        if(pluginIds.contains(pluginId)) {
          usages += Json.obj(
            "project" -> project.name.toString,
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

object PluginApi {

  private final val taskPluginsExampleJson =
"""
{
  "multiCsv" : {
    "title" : "Multi CSV ZIP",
    "categories" : [ "file" ],
    "description" : "Reads from or writes to multiple CSV files from/to a single ZIP file.",
    "markdownDocumentation": "# Some markdown documentations"
  },
  "csv" : {
    "title" : "CSV",
    "categories" : [ "file" ],
    "description" : "Read from or write to an CSV file."
  },
  ...
}
"""

 private final val pluginDescriptionExampleJson =
"""
{
  "title" : "Transform",
  "categories" : [ "Transform" ],
  "description" : "A transform task defines a mapping from a source structure to a target structure.",
  "taskType" : "Transform",
  "type" : "object",
  "pluginId": "transform",
  "properties" : {
    "selection" : {
      "title" : "Input task",
      "description" : "The source from which data will be transformed when executed as a single task outside of a workflow.",
      "type" : "object",
      "parameterType" : "objectParameter",
      "value" : null,
      "advanced" : false,
      "visibleInDialog" : true,
      "pluginId" : "datasetSelectionParameter",
      "properties" : {
        "inputId" : {
          "title" : "Dataset",
          "description" : "The dataset to select.",
          "type" : "string",
          "parameterType" : "identifier",
          "value" : null,
          "advanced" : false,
          "visibleInDialog" : true,
          "autoCompletion" : {
            "allowOnlyAutoCompletedValues" : true,
            "autoCompleteValueWithLabels" : true,
            "autoCompletionDependsOnParameters" : [ ]
          }
        },
        "typeUri" : {
          "title" : "Type",
          "description" : "The type of the dataset. If left empty, the default type will be selected.",
          "type" : "string",
          "parameterType" : "uri",
          "value" : null,
          "advanced" : false,
          "visibleInDialog" : true,
          "autoCompletion" : {
            "allowOnlyAutoCompletedValues" : false,
            "autoCompleteValueWithLabels" : false,
            "autoCompletionDependsOnParameters" : [ ]
          }
        },
        "restriction" : {
          "title" : "Restriction",
          "description" : "Additional restrictions on the enumerated entities. If this is an RDF source, use SPARQL patterns that include the variable ?a to identify the enumerated entities, e.g. ?a foaf:knows <http://example.org/SomePerson>",
          "type" : "string",
          "parameterType" : "restriction",
          "value" : "",
          "advanced" : false,
          "visibleInDialog" : true
        }
      }
    },
    "mappingRule" : {
      "title" : "mapping rule",
      "description" : "",
      "type" : "object",
      "parameterType" : "objectParameter",
      "value" : {
        "type" : "root",
        "id" : "root",
        "rules" : {
          "uriRule" : null,
          "typeRules" : [ ],
          "propertyRules" : [ ]
        },
        "metadata" : {
          "label" : "Root Mapping"
        }
      },
      "advanced" : false,
      "visibleInDialog" : false
    },
    "output" : {
      "title" : "Output dataset",
      "description" : "An optional dataset where the transformation results should be written to when executed as single task outside of a workflow.",
      "type" : "string",
      "parameterType" : "option[identifier]",
      "value" : "",
      "advanced" : false,
      "visibleInDialog" : true,
      "autoCompletion" : {
        "allowOnlyAutoCompletedValues" : true,
        "autoCompleteValueWithLabels" : true,
        "autoCompletionDependsOnParameters" : [ ]
      }
    },
    ... SNIP ...
  },
  "required" : [ "selection" ]
}
"""

  private final val pluginJsonDescription =
"""
Contains the typical meta data of a plugin like title, categories and description.
The 'taskType' property is optional and specifies the task type a task related plugin, e.g. workflow, dataset etc., belongs to.
The task type must be specified when creating tasks via the generic /tasks endpoint.
The JSON schema part of the plugin parameters is described in the 'properties' object. Besides title
and description each parameter has the JSON type, which can only be "string" or "object" at the moment.
The 'parameterType' specifies the internal data type. For "object" types this can be ignored, for "string"
parameter types this gives a hint at what kind of UI widget is appropriate and what kind of validation could be applied.
The 'value' property gives the default value when this parameter is not specified.
The 'advanced' property marks the parameter as advanced and acts as a hint that this parameter should
be somehow handled differently by the UI.
If the 'visibleInDialog' property is set to false, then this parameter should not be set from a creation
or update dialog. Usually this parameter is complex and is modified in special editors, e.g. the mapping editor.
The pluginId property specifies the ID of the plugin and is also set for all plugin parameters that
are plugins themselves. The plugin ID is needed, e.g. for the auto-completion of parameter values.
A parameter can have an autoCompletion property that specifies how a parameter value can or should be auto-completed.
If allowOnlyAutoCompletedValues is set to true then the UI must make sure that only values from the auto-completion
are considered as valid.
If autoCompleteValueWithLabels is set to true, then the auto-completion values might have a label in addition
to the actual value. Only the label should be presented to the user then.
The autoCompletionDependsOnParameters array specifies the values of parameters from the same object,
a specific parameter depends on. These must be send in the auto-completion request in the same order.
"""

  private final val pluginUsagesExample =
"""
[
  {
    "project": "projectId",
    "task": "taskId",
    "link": "{editor URL}"
  }
]
"""

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
