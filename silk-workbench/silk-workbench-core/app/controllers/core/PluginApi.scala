package controllers.core

import org.silkframework.runtime.plugin.{Parameter, ParameterType, PluginRegistry}
import org.silkframework.util.StringUtils
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

class PluginApi extends Controller {

  def plugins(pluginType: String) = Action {
    Ok(PluginSerializer.pluginsAsJson(pluginType))
  }

  private object PluginSerializer {

    def pluginsAsJson(pluginType: String) = {
      val pluginClass = getClass.getClassLoader.loadClass(pluginType)
      val plugins = PluginRegistry.availablePluginsForClass(pluginClass)

      JsArray(
        for(plugin <- plugins) yield {
          Json.obj(
            "title" -> JsString(plugin.id),
            "description" -> JsString(plugin.description),
            "type" -> "object",
            "properties" -> JsObject(serializeParams(plugin.parameters)),
            "required" -> JsArray(plugin.parameters.filterNot(_.defaultValue.isDefined).map(_.name).map(JsString))
          )
        }
      )
    }

    private def serializeParams(params: Seq[Parameter]) = {
      for(param <- params) yield {
        param.name -> serializeParam(param)
      }
    }

    private def serializeParam(param: Parameter) = {
      val defaultValue = param.stringDefaultValue match {
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
