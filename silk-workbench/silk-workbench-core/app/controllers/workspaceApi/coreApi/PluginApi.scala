package controllers.workspaceApi.coreApi

import controllers.util.{SerializationUtils, TextSearchUtils}
import javax.inject.Inject
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.{PluginDescription, PluginList, PluginRegistry}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.PluginSerializers.PluginListJsonFormat
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc._

import scala.collection.immutable.ListMap

/**
  * Workspace task plugin related endpoints.
  */
class PluginApi @Inject()(pluginCache: PluginApiCache) extends InjectedController {
  /** All plugins that can be created in the workspace. */
  def taskPlugins(addMarkdownDocumentation: Boolean,
                  textQuery: Option[String],
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
  def plugin(pluginId: String,
             addMarkdownDocumentation: Boolean,
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
      JsonSerializers.TASK_TYPE_CUSTOM_TASK -> classOf[CustomTask],
      JsonSerializers.TASK_TYPE_WORKFLOW -> classOf[Workflow],
      JsonSerializers.TASK_TYPE_TRANSFORM -> classOf[TransformSpec],
      JsonSerializers.TASK_TYPE_LINKING -> classOf[LinkSpec]
    )
    taskTypes.find(_._2.isAssignableFrom(pluginClass)).map(_._1)
  }
}