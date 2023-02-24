package org.silkframework.serialization.json

import org.silkframework.runtime.plugin.{AnyPlugin, ParameterObjectValue, ParameterStringValue, ParameterTemplateValue,
  ParameterValue, ParameterValues, PluginDescription, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.{PARAMETERS, TEMPLATES, TYPE}
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, Json}

import scala.reflect.ClassTag

object PluginSerializers {

  /**
    * (De-)Serializes plugin parameter values.
    */
  implicit object ParameterValuesJsonFormat extends JsonFormat[ParameterValues] {

    override def read(value: JsValue)(implicit readContext: ReadContext): ParameterValues = {
      val parameters = ParameterValues((value \ PARAMETERS).as[JsObject].value.mapValues(readParameters).toMap)
      val templates = ParameterValues((value \ TEMPLATES).as[JsObject].value.mapValues(readTemplates).toMap)
      parameters merge templates
    }

    override def write(value: ParameterValues)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        PARAMETERS -> writeParameters(value),
        TEMPLATES -> writeTemplates(value)
      )
    }

    private def readParameters(value: JsValue): ParameterValue = {
      value match {
        case boolean: JsBoolean => ParameterStringValue(boolean.value.toString)
        case str: JsString => ParameterStringValue(str.value)
        case number: JsNumber => ParameterStringValue(number.value.toString())
        case obj: JsObject => ParameterValues(obj.value.mapValues(readParameters).toMap)
        case other: JsValue =>
          throw new IllegalArgumentException(s"Values of type '${other.getClass.getSimpleName}' are not supported as parameter values!")
      }
    }

    private def readTemplates(value: JsValue): ParameterValue = {
      value match {
        case str: JsString => ParameterTemplateValue(str.value)
        case obj: JsObject => ParameterValues(obj.value.mapValues(readTemplates).toMap)
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
          case (key, ParameterObjectValue(objValue)) =>
            (key, Serialization.formatForDynamicType[JsValue](objValue.getClass).write(objValue))
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
      val id = (value \ TYPE).as[JsString].value
      val params = ParameterValuesJsonFormat.read(value) merge extraParameters

      pluginDesc match {
        case Some(plugin) =>
          plugin(params)
        case None =>
          PluginRegistry.create[T](id, params)
      }
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): T = {
      read(value, ParameterValues.empty)
    }

    override def write(value: T)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        TYPE -> JsString(value.pluginSpec.id.toString),
      ) ++ ParameterValuesJsonFormat.write(value.parameters)
    }
  }
}
