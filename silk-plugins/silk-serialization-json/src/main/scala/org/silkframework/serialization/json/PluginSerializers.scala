package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{Parameter, PluginDescription, PluginList}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.serialization.WriteContext
import play.api.libs.json._

object PluginSerializers {
  final val MARKDOWN_DOCUMENTATION_PARAMETER = "markdownDocumentation"

  /**
    * Generates a JSON serialization of a list of plugins.
    * The returned JSON format stays as close to JSON Schema as possible.
    */
  implicit object PluginListJsonFormat extends WriteOnlyJsonFormat[PluginList] {

    override def write(pluginList: PluginList)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        for((_, plugins) <- pluginList.pluginsByType; plugin <- plugins) yield {
          plugin.id.toString -> serializePlugin(plugin, pluginList.serializeMarkdownDocumentation)
        }
      )
    }

    private def serializePlugin(plugin: PluginDescription[_], withMarkdownDocumentation: Boolean): JsObject = {
      val markdownDocumentation = if(withMarkdownDocumentation && plugin.documentation.nonEmpty){
        Some((MARKDOWN_DOCUMENTATION_PARAMETER -> JsString(plugin.documentation)))
      } else { None }
      JsObject(
        Seq(
          "title" -> JsString(plugin.label),
          "categories" -> JsArray(plugin.categories.map(JsString)),
          "description" -> JsString(plugin.description),
          "type" -> JsString("object"),
          "properties" -> JsObject(serializeParams(plugin.parameters)),
          "required" -> JsArray(plugin.parameters.filterNot(_.defaultValue.isDefined).map(_.name).map(JsString))
        ) ++ markdownDocumentation
      )
    }

    private def serializeParams(params: Seq[Parameter]) = {
      for(param <- params) yield {
        param.name -> serializeParam(param)
      }
    }

    private def serializeParam(param: Parameter) = {
      val defaultValue = param.stringDefaultValue(Prefixes.empty, EmptyResourceManager()) match {
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
