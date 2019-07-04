package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{Parameter, PluginDescription, PluginList}
import org.silkframework.runtime.serialization.WriteContext
import play.api.libs.json._

object PluginSerializers {

  /**
    * Generates a JSON serialization of a list of plugins.
    * The returned JSON format stays as close to JSON Schema as possible.
    */
  implicit object PluginListJsonFormat extends WriteOnlyJsonFormat[PluginList] {

    override def write(plugins: PluginList)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        for((pluginType, plugins) <- plugins.pluginsByType; plugin <- plugins) yield {
          plugin.id.toString -> serializePlugin(plugin)
        }
      )
    }

    private def serializePlugin(plugin: PluginDescription[_]) = {
      Json.obj(
        "title" -> JsString(plugin.label),
        "categories" -> plugin.categories.map(JsString),
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
        "value" -> defaultValue,
        "advanced" -> JsBoolean(param.advanced)
      )
    }
  }

}
