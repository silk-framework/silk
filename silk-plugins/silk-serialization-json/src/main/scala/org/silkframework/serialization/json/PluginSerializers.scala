package org.silkframework.serialization.json

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.serialization.{Serialization, WriteContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectTrait, WorkspaceReadTrait}
import play.api.libs.json._

import java.util.logging.{Level, Logger}
import scala.util.control.NonFatal

object PluginSerializers {
  final val MARKDOWN_DOCUMENTATION_PARAMETER = "markdownDocumentation"

  /**
    * Generates a JSON serialization of a list of plugins.
    * The returned JSON format stays as close to JSON Schema as possible.
    */
  implicit object PluginListJsonFormat extends WriteOnlyJsonFormat[PluginList] {

    private lazy val log = Logger.getLogger(getClass.getName)

    override def write(pluginList: PluginList)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        for((_, plugins) <- pluginList.pluginsByType; plugin <- plugins) yield {
          plugin.id.toString -> serializePlugin(plugin, pluginList.serializeMarkdownDocumentation, pluginList.overviewOnly, taskType = None, withLabels = false)
        }
      )
    }

    def serializePlugin(plugin: PluginDescription[_],
                        withMarkdownDocumentation: Boolean,
                        overviewOnly: Boolean,
                        taskType: Option[String],
                        withLabels: Boolean)
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
        "properties" -> JsObject(serializeParams(plugin.parameters, withLabels)),
        "required" -> JsArray(plugin.parameters.filterNot(_.defaultValue.isDefined).map(_.name).map(JsString)),
        "pluginId" -> JsString(plugin.id)
      ).filter(_ => !overviewOnly)
      JsObject(metaData ++ tt ++ details ++ markdownDocumentation)
    }

    private def serializeParams(params: Seq[PluginParameter],
                                withLabels: Boolean)
                               (implicit writeContext: WriteContext[JsValue]): Seq[(String, JsValue)] = {
      for(param <- params) yield {
        param.name -> serializeParam(param, withLabels)
      }
    }

    private def serializeParam(param: PluginParameter,
                               withLabels: Boolean)
                              (implicit writeContext: WriteContext[JsValue]): JsValue = {
      val defaultValue: JsValue = defaultValueToJs(param, withLabels)
      val pluginId: Option[String] = param.parameterType match {
        case objectType: PluginObjectParameterTypeTrait => objectType.pluginDescription.map(_.id.toString)
        case _ => None
      }

      val (parameters, requiredList): (Option[JsObject], Option[Seq[String]]) = param.parameterType match {
        case objectType: PluginObjectParameterTypeTrait if param.visibleInDialog =>
          val pluginDescription = ClassPluginDescription(objectType.pluginObjectParameterClass)
          val parameters = JsObject(pluginDescription.parameters.map(p => p.name -> serializeParam(p, withLabels)))
          val requiredParameters = pluginDescription.parameters.filterNot(_.defaultValue.isDefined).map(_.name)
          (Some(parameters), Some(requiredParameters))
        case _ => (None, None)
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
        pluginId = pluginId,
        required = requiredList
      ))
    }

    private def defaultValueToJs(param: PluginParameter,
                                 withLabels: Boolean)
                                (implicit writeContext: WriteContext[JsValue]): JsValue = {
      (param.parameterType, param.defaultValue) match {
        case (objectType: PluginObjectParameterTypeTrait, Some(v)) =>
          val value = serializeParameterValue(objectType.pluginObjectParameterClass, v)
          if (withLabels) {
            Json.obj("value" -> value)
          } else {
            value
          }
        case (_, Some(_)) =>
          val value = param.stringDefaultValue.get
          param.autoCompletion match {
            case Some(autoCompletion) if withLabels =>
              val label: Option[String] = if(autoCompletion.autoCompleteValueWithLabels) {
                // No project ID/workspace available, only add label for parameters that do not need project/workspace information
                try {
                  autoCompletion.autoCompletionProvider.valueToLabel(writeContext.projectId.map(_.toString).getOrElse("dummy"),
                    value, Seq.empty, DummyWorkspaceRead)(UserContext.Empty)
                } catch {
                  case NonFatal(ex) =>
                    log.log(Level.WARNING, s"Could not retrieve label for default parameter value '$value'", ex)
                    None
                }
              } else {
                None
              }
              Json.obj(
                "value" -> value,
                "label" -> label
              )
            case _ =>
              JsString(value)
          }
        case (_, None) => JsNull
      }
    }

    def serializeParameterValue(pluginObjectParameterClass: Class[_],
                                value: AnyRef)
                               (implicit writeContext: WriteContext[JsValue]): JsValue = {
      val jsonFormat = Serialization.formatForDynamicType[JsValue](pluginObjectParameterClass)
      jsonFormat.write(value)
    }
  }

  object DummyWorkspaceRead extends WorkspaceReadTrait {
    override def projects(implicit userContext: UserContext): Seq[ProjectTrait] = Seq.empty
    override def project(name: Identifier)(implicit userContext: UserContext): ProjectTrait = throw new RuntimeException("Cannot retrieve projects!")
    override def findProject(name: Identifier)(implicit userContext: UserContext): Option[ProjectTrait] = None
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
  * @param required For object parameter types it will list the required parameters.
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
                                      pluginId: Option[String],
                                      required: Option[Seq[String]])

case class ParameterAutoCompletionJsonPayload(allowOnlyAutoCompletedValues: Boolean,
                                              autoCompleteValueWithLabels: Boolean,
                                              autoCompletionDependsOnParameters: Seq[String])

object PluginParameterJsonPayload {
  implicit val parameterAutoCompletionJsonPayloadFormat: Format[ParameterAutoCompletionJsonPayload] = Json.format[ParameterAutoCompletionJsonPayload]
  implicit val pluginParameterJsonPayloadFormat: Format[PluginParameterJsonPayload] = Json.format[PluginParameterJsonPayload]
}
