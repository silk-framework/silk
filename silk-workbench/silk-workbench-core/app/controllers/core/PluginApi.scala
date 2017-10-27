package controllers.core

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{Parameter, ParameterType, PluginDescription, PluginRegistry}
import org.silkframework.util.StringUtils
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

class PluginApi extends Controller {

  def plugins(pluginType: String) = Action {
    Ok(PluginSerializer.pluginsAsJson(pluginType))
  }

  /**
    * Generates a JSON serialization of the available plugins.
    * The returned JSON format stays as close to JSON Schema as possible.
    */
  private object PluginSerializer {

    def pluginsAsJson(pluginType: String): JsObject = {
      val pluginClass = getClass.getClassLoader.loadClass(pluginType)
      val plugins = PluginRegistry.availablePluginsForClass(pluginClass)

      JsObject(
        for(plugin <- plugins) yield {
          plugin.id.toString -> serializePlugin(plugin)
        }
      )
    }

    private def serializePlugin(plugin: PluginDescription[_]) = {
      Json.obj(
        "title" -> JsString(plugin.label),
        "description" -> JsString(plugin.description),
        "type" -> "object",
        "properties" -> JsObject(serializeParams(plugin.parameters)),
        "required" -> JsArray(plugin.parameters.filterNot(_.defaultValue.isDefined).map(_.name).map(JsString))
      )
    }

    private def serializeParams(params: Seq[Parameter]) = {
      for(param <- params) yield {
        param.name -> serializeParam(param)
      }
    }

    private def serializeParam(param: Parameter) = {
      val defaultValue = param.stringDefaultValue(Prefixes.empty) match {
        case Some(value) => JsString(value)
        case None => JsNull
      }

      Json.obj(
        "title" -> JsString(param.label),
        "description" -> JsString(param.description),
        "type" -> JsString(param.dataType.name),
        "value" -> defaultValue
      )
    }
  }

}
