package controllers.workspaceApi.coreApi

import controllers.util.{SerializationUtils, TextSearchUtils}
import javax.inject.Inject
import org.silkframework.config.CustomTask
import org.silkframework.dataset.Dataset
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.{PluginDescription, PluginList, PluginRegistry}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.PluginSerializers.PluginListJsonFormat
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.collection.immutable.ListMap

/**
  * Workspace task plugin related endpoints.
  */
class PluginApi @Inject() () extends InjectedController {
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
    ) flatMap (pl => PluginRegistry.pluginDescriptionById(pl))
    val singlePluginsList = PluginList(ListMap("singleTasks" -> singlePlugins), addMarkdownDocumentation, overviewOnly = true)
    pluginResult(addMarkdownDocumentation, pluginTypes, singlePluginsList, textQuery, category)
  }

  /** Return plugin description of a single plugin. */
  def plugin(pluginId: String,
             addMarkdownDocumentation: Boolean,
             pretty: Boolean): Action[AnyContent] = Action { implicit request =>
    PluginRegistry.pluginDescriptionById(pluginId) match {
      case Some(pluginDesc) =>
        implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
        val resultJson = PluginListJsonFormat.serializePlugin(pluginDesc, addMarkdownDocumentation, overviewOnly = false, taskType = taskType(pluginDesc.pluginClass))
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

  private def taskType(pluginClass: Class[_]): Option[String] = {
    val taskTypes = Seq(
      JsonSerializers.TASK_TYPE_DATASET -> classOf[Dataset],
      JsonSerializers.TASK_TYPE_CUSTOM_TASK -> classOf[CustomTask],
      JsonSerializers.TASK_TYPE_WORKFLOW -> classOf[Workflow],
      JsonSerializers.TASK_TYPE_TRANSFORM -> classOf[TransformSpec],
      JsonSerializers.TASK_TYPE_LINKING -> classOf[LinkSpec]
    )
    taskTypes.find(_._2.isAssignableFrom(pluginClass)).map(_._1)
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
    SerializationUtils.serializeCompileTime(pluginList.copy(pluginsByType = filteredPlugins), None)
  }
}
