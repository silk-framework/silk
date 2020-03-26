package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{Parameter, PluginDescription, PluginList}
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

    private def serializeParams(params: Seq[Parameter]): Seq[(String, JsValue)] = {
      for(param <- params) yield {
        param.name -> serializeParam(param)
      }
    }

    private def serializeParam(param: Parameter): JsValue = {
      val defaultValue: JsValue = param.stringDefaultValue(Prefixes.empty) match {
        case Some(value) => JsString(value)
        case None => JsNull
      }

      Json.toJson(PluginParameterJsonPayload(
        title = param.label,
        description = param.description,
        `type` = param.dataType.name,
        value = defaultValue,
        advanced = param.advanced,
        autoCompleteSupport = param.autoCompletionSupport,
        // Don't output auto-complete specific parameters, if auto-completion is disabled.
        allowOnlyAutoCompletedValues = Some(param.allowOnlyAutoCompletedValues).filter(_ => param.autoCompletionSupport),
        autoCompleteValueWithLabels = Some(param.autoCompleteValueWithLabels).filter(_ => param.autoCompletionSupport)
      ))
    }
  }
}

case class PluginParameterJsonPayload(title: String,
                                      description: String,
                                     `type`: String,
                                      value: JsValue,
                                      advanced: Boolean,
                                      autoCompleteSupport: Boolean,
                                      allowOnlyAutoCompletedValues: Option[Boolean],
                                      autoCompleteValueWithLabels: Option[Boolean])

object PluginParameterJsonPayload {
  implicit val pluginParameterJsonPayloadFormat: Format[PluginParameterJsonPayload] = Json.format[PluginParameterJsonPayload]
}
