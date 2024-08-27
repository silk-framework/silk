package org.silkframework.serialization.json

import org.silkframework.runtime.plugin._
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.{PARAMETERS, TEMPLATES, TYPE}
import play.api.libs.json._

import scala.reflect.ClassTag

object PluginSerializers {

  /**
    * (De-)Serializes plugin parameter values.
    */
  implicit object ParameterValuesJsonFormat extends JsonFormat[ParameterValues] {

    override def read(value: JsValue)(implicit readContext: ReadContext): ParameterValues = {
      val parameters = ParameterValues((value \ PARAMETERS).as[JsObject].value.view.mapValues(readParameters).toMap)
      val templates = ParameterValues((value \ TEMPLATES).asOpt[JsObject].map(_.value.view.mapValues(readTemplates).toMap).getOrElse(Map.empty))
      parameters merge templates
    }

    override def write(value: ParameterValues)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      val parameters = writeParameters(value)
      val templates = writeTemplates(value)
      if(templates.value.isEmpty) {
        Json.obj(
          PARAMETERS -> parameters
        )
      } else {
        Json.obj(
          PARAMETERS -> parameters,
          TEMPLATES -> templates
        )
      }
    }

    private def readParameters(value: JsValue): ParameterValue = {
      value match {
        case boolean: JsBoolean => ParameterStringValue(boolean.value.toString)
        case str: JsString => ParameterStringValue(str.value)
        case number: JsNumber => ParameterStringValue(number.value.toString())
        case obj: JsObject => ParameterValues(obj.value.view.mapValues(readParameters).toMap)
        case array: JsArray => ParameterValues.empty
        case JsNull => ParameterValues.empty
      }
    }

    private def readTemplates(value: JsValue): ParameterValue = {
      value match {
        case str: JsString => ParameterTemplateValue(str.value)
        case obj: JsObject => ParameterValues(obj.value.view.mapValues(readTemplates).toMap)
        case other: JsValue =>
          throw new IllegalArgumentException(s"Values of type '${other.getClass.getSimpleName}' are not supported as template values!")
      }
    }

    private def writeParameters(params: ParameterValues)
                               (implicit writeContext: WriteContext[JsValue]): JsObject = {
      JsObject(
        params.values.collect {
          case (key, ParameterStringValue(strValue)) =>
            (key, JsString(strValue))
          case (key, template: ParameterTemplateValue) =>
            (key, JsString(template.evaluate(writeContext.templateVariables.all)))
          case (key, parameterObjectValue: ParameterObjectValue) =>
            val value = parameterObjectValue.value(writeContext)
            (key, Serialization.formatForDynamicType[JsValue](value.getClass).write(value))
          case (key, values: ParameterValues) =>
            (key, writeParameters(values))
        }
      )
    }

    private def writeTemplates(params: ParameterValues): JsObject = {
      JsObject(
        params.values.collect {
          case (key, ParameterTemplateValue(strValue)) =>
            (key, JsString(strValue))
          case (key, values: ParameterValues) =>
            (key, writeTemplates(values))
        }
      )
    }
  }

  /**
    * (De-)Serializes plugins of a provided base type.
    *
    * @tparam T Plugin base type
    */
  class PluginJsonFormat[T <: AnyPlugin : ClassTag](pluginDesc: Option[PluginDescription[T]] = None) extends JsonFormat[T] {

    def read(value: JsValue, extraParameters: ParameterValues)(implicit readContext: ReadContext): T = {
      val params = ParameterValuesJsonFormat.read(value) merge extraParameters

      pluginDesc match {
        case Some(plugin) =>
          plugin(params)
        case None =>
          val id = (value \ TYPE).as[JsString].value
          PluginRegistry.create[T](id, params)
      }
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): T = {
      read(value, ParameterValues.empty)
    }

    override def write(value: T)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      // The JSON serialization currently should use prefixed names, so we need to re-serialize the parameters with prefixes.
      val parameters = value.pluginSpec.parameterValues(value)
      Json.obj(
        TYPE -> JsString(value.pluginSpec.id.toString),
      ) ++ ParameterValuesJsonFormat.write(parameters)
    }

  }
}
