package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{Parameter, PluginDescription, PluginList, PluginObjectParameterTypeTrait}
import org.silkframework.runtime.serialization.{Serialization, WriteContext}
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
          plugin.id.toString -> serializePlugin(plugin, pluginList.serializeMarkdownDocumentation, pluginList.overviewOnly, taskType = None)
        }
      )
    }

    def serializePlugin(plugin: PluginDescription[_], withMarkdownDocumentation: Boolean, overviewOnly: Boolean, taskType: Option[String])
                       (implicit writeContext: WriteContext[JsValue]): JsObject = {
      val markdownDocumentation = if(withMarkdownDocumentation && plugin.documentation.nonEmpty){
        Some((MARKDOWN_DOCUMENTATION_PARAMETER -> JsString(plugin.documentation)))
      } else { None }
      val metaData = Seq(
        "title" -> JsString(plugin.label),
        "categories" -> JsArray(plugin.categories.map(JsString)),
        "description" -> JsString(plugin.description)
      )
      val tt = taskType.map(t => JsonSerializers.TASKTYPE -> JsString(t)).toSeq
      val details = Seq (
        "type" -> JsString("object"),
        "properties" -> JsObject(serializeParams(plugin.parameters)),
        "required" -> JsArray(plugin.parameters.filterNot(_.defaultValue.isDefined).map(_.name).map(JsString)),
        "pluginId" -> JsString(plugin.id)
      ).filter(_ => !overviewOnly)
      JsObject(metaData ++ tt ++ details ++ markdownDocumentation)
    }

    private def serializeParams(params: Seq[Parameter])
                               (implicit writeContext: WriteContext[JsValue]): Seq[(String, JsValue)] = {
      for(param <- params) yield {
        param.name -> serializeParam(param)
      }
    }

    private def serializeParam(param: Parameter)
                              (implicit writeContext: WriteContext[JsValue]): JsValue = {
      val defaultValue: JsValue = (param.parameterType, param.defaultValue) match {
        case (objectType: PluginObjectParameterTypeTrait, Some(v)) =>
          serializeParameterValue(objectType.pluginObjectParameterClass, v)
        case (_, Some(_)) =>
          JsString(param.stringDefaultValue(Prefixes.empty).get)
        case (_, None) => JsNull
      }
      val pluginId: Option[String] = param.parameterType match {
        case objectType: PluginObjectParameterTypeTrait => objectType.pluginDescription.map(_.id.toString)
        case _ => None
      }

      val parameters: Option[JsObject] = param.parameterType match {
        case objectType: PluginObjectParameterTypeTrait if param.visibleInDialog =>
          val pluginDescription = PluginDescription(objectType.pluginObjectParameterClass)
          Some(JsObject(pluginDescription.parameters.map(p => (p.name -> serializeParam(p)))))
        case _ => None
      }
      Json.toJson(PluginParameterJsonPayload(
        title = param.label,
        description = param.description,
        `type` = param.parameterType.jsonSchemaType,
        parameterType = param.parameterType.name,
        value = defaultValue,
        advanced = param.advanced,
        visibleInDialog = param.visibleInDialog,
        autoCompletion = param.autoCompletion.map(autoComplete => ParameterAutoCompletionJsonPayload(
          allowOnlyAutoCompletedValues = autoComplete.allowOnlyAutoCompletedValues,
          autoCompleteValueWithLabels = autoComplete.autoCompleteValueWithLabels,
          autoCompletionDependsOnParameters = autoComplete.autoCompletionDependsOnParameters
        )),
        properties = parameters,
        pluginId = pluginId
      ))
    }

    def serializeParameterValue(pluginObjectParameterClass: Class[_],
                                value: AnyRef)
                               (implicit writeContext: WriteContext[JsValue]): JsValue = {
      val jsonFormat = Serialization.formatForDynamicType[JsValue](pluginObjectParameterClass)
      jsonFormat.write(value)
    }
  }
}

/**
  * @param title           Human-readable title of the parameter.
  * @param description     Description of the parameter.
  * @param `type`          The JSON type of the parameter, at the moment either "string" or "object".
  * @param parameterType   The internal parameter type, coming from [[org.silkframework.runtime.plugin.ParameterType]]
  * @param value           The value of the parameter.
  * @param advanced        If this parameter is marked as advanced and should be hidden behind an 'advanced' menu in the UI.
  * @param visibleInDialog If this parameter should be represented in a UI dialog. If set to false the parameter must not be shown in the dialog and no value must be set in any backend request.
  * @param autoCompletion  Optional auto-completion information.
  * @param properties      Optional properties if this is a nested object parameter that can be shown in the UI.
  * @param pluginId        Optional plugin ID, if this parameter is itself a plugin.
  */
case class PluginParameterJsonPayload(title: String,
                                      description: String,
                                     `type`: String,
                                      parameterType: String,
                                      value: JsValue,
                                      advanced: Boolean,
                                      visibleInDialog: Boolean,
                                      autoCompletion: Option[ParameterAutoCompletionJsonPayload],
                                      properties: Option[JsObject],
                                      pluginId: Option[String])

case class ParameterAutoCompletionJsonPayload(allowOnlyAutoCompletedValues: Boolean,
                                              autoCompleteValueWithLabels: Boolean,
                                              autoCompletionDependsOnParameters: Seq[String])

object PluginParameterJsonPayload {
  implicit val parameterAutoCompletionJsonPayloadFormat: Format[ParameterAutoCompletionJsonPayload] = Json.format[ParameterAutoCompletionJsonPayload]
  implicit val pluginParameterJsonPayloadFormat: Format[PluginParameterJsonPayload] = Json.format[PluginParameterJsonPayload]
}
