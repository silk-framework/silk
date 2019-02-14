package controllers.core

import controllers.util.SerializationUtils
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.{JsonFormat, WriteOnlyJsonFormat}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Request}

import scala.collection.immutable.ListMap

class PluginApi extends Controller {

  def plugins() = Action { implicit request => {
    val pluginTypes = Seq(
      "org.silkframework.workspace.WorkspaceProvider",
      "org.silkframework.workspace.resources.ResourceRepository",
      "org.silkframework.config.CustomTask",
      "org.silkframework.dataset.Dataset",
      "org.silkframework.rule.similarity.DistanceMeasure",
      "org.silkframework.rule.input.Transformer",
      "org.silkframework.rule.similarity.Aggregator"
    )
    val pluginList = PluginList.load(pluginTypes)

    generate(pluginList)
  }}

  def pluginsForTypes(pluginType: String) = Action { implicit request => {
    val pluginTypes = pluginType.split("\\s*,\\s*")
    val pluginList = PluginList.load(pluginTypes)

   generate(pluginList)
  }}

  private def generate(pluginList: PluginList)(implicit request: Request[AnyContent]) = {
    if(request.accepts("text/csv") || request.accepts("application/csv")) {
      Ok(PluginCsvSerializer(pluginList))
    } else {
      SerializationUtils.serializeCompileTime(pluginList, None)
    }
  }

  private object PluginCsvSerializer {

    private val sep = ';'

    private val arraySep = ','

    def apply(plugins: PluginList): String = {
      val sb = new StringBuilder()

      sb ++= s"Identifier${sep}Label${sep}Description${sep}Parameters${sep}Categories${sep}Namespace${sep}Plugin Type\n"

      for((pluginType, plugins) <- plugins.pluginsByType; plugin <- plugins) {
        sb ++= plugin.id
        sb += sep
        sb ++= escape(plugin.label)
        sb += sep
        sb ++= escape(plugin.description)
        sb += sep
        sb ++= escape(plugin.parameters.map(serializeParameter).mkString("\n"))
        sb += sep
        sb ++= escape(plugin.categories)
        sb += sep
        sb ++= plugin.pluginClass.getPackage.getName
        sb += sep
        sb ++= pluginType
        sb ++= "\n"
      }

      sb.toString()
    }

    private def serializeParameter(param: Parameter): String = {
      val sb = new StringBuilder()
      sb ++= param.label
      sb ++= ": "
      sb ++= param.description
      sb.toString
    }


    def escape(value: String): String = {
      "\"" + value.replace("\"", "\"\"") + "\""
    }

    def escape(values: Traversable[String]): String = {
      escape(values.mkString(arraySep.toString))
    }


  }

}
