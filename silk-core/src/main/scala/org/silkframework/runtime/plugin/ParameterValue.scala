package org.silkframework.runtime.plugin

import org.silkframework.runtime.serialization.{ReadContext, Serialization, SerializationFormat, WriteContext}
import org.silkframework.runtime.templating.TemplateVariables

import scala.xml.Node

/**
  * The value of a plugin parameter.
  */
sealed trait ParameterValue

/**
  * A string parameter value.
  */
case class ParameterStringValue(value: String) extends ParameterValue

/**
  * An object parameter value.
  * Can be used for parameters that cannot be represented as strings.
  *
  * @param xmlFormat        The XML format the parameter value object was serialized with.
  * @param xmlSerialization The XML serialization of the parameter value object.
  *
  */
case class ParameterObjectValue(valueOrSerialization: Either[AnyRef, ParameterObjectValueSerialized]) extends ParameterValue {
  /** Deserializes the parameter value with the given plugin context. */
  def value(pluginContext: PluginContext): AnyRef = {
    valueOrSerialization match {
      case Left(v) => v
      case Right(ParameterObjectValueSerialized(xmlFormat, xmlSerialization)) =>
        implicit val readContext: ReadContext = ReadContext.fromPluginContext()(pluginContext)
        xmlFormat.read(xmlSerialization).asInstanceOf[AnyRef]
    }
  }
}

case class ParameterObjectValueSerialized(xmlFormat: SerializationFormat[Any, Node], xmlSerialization: Node)

object ParameterObjectValue {
  def apply(parameterValue: AnyRef)
           (implicit  pluginContext: PluginContext): ParameterObjectValue = {
    try {
      val xmlFormat = Serialization.formatForDynamicType[Node](parameterValue.getClass)
      implicit val writeContext: WriteContext[Node] = WriteContext.fromPluginContext[Node]()(pluginContext)
      val valueXml = xmlFormat.write(parameterValue)
      ParameterObjectValue(Right(ParameterObjectValueSerialized(xmlFormat, valueXml)))
    } catch {
      case _: NoSuchElementException =>
        ParameterObjectValue(Left(parameterValue))
    }

  }
}

/**
  * A template parameter value.
  */
case class ParameterTemplateValue(template: String) extends ParameterValue {

  def evaluate(templateVariables: TemplateVariables): String = {
    templateVariables.resolveTemplateValue(template)
  }

}

/**
  * Holds nested parameter values.
  */
case class ParameterValues(values: Map[String, ParameterValue]) extends ParameterValue {

  /**
    * Converts the root parameter values to a string map.
    * Nested values are ignored.
    */
  def toStringMap(implicit pluginContext: PluginContext): Map[String, String] = {
    values.collect {
      case (key, ParameterStringValue(value)) =>
        (key, value)
      case (key, template: ParameterTemplateValue) =>
        (key, template.evaluate(pluginContext.templateVariables.all))
    }
  }

  /**
    * Returns the root templates as a string map.
    * Nested templates are ignored.
    */
  def templates: Map[String, String] = {
    values.collect { case (key, t: ParameterTemplateValue) => (key, t.template)}
  }

  /**
    * Only returns the nested templates.
    */
  def filterTemplates: ParameterValues = {
    copy(values =
      values.collect {
        case (key, template: ParameterTemplateValue) =>
          (key, template)
        case (key, values: ParameterValues) =>
          (key, values.filterTemplates)
      }
    )
  }

  /**
    * Merges this parameter values tree with another value tree.
    */
  def merge(other: ParameterValues): ParameterValues = {
    val updatedValues =
      for((key, value) <- values) yield {
        val otherValue = other.values.get(key) match {
          case Some(otherNestedValues: ParameterValues) =>
            value match {
              case nestedValues: ParameterValues =>
                nestedValues.merge(otherNestedValues)
              case _ =>
                otherNestedValues
            }
          case Some(otherSimpleValue) =>
            otherSimpleValue
          case None =>
            value
        }
        (key, otherValue)
      }

    ParameterValues(updatedValues ++ (other.values -- updatedValues.keys))
  }

}

object ParameterValues {

  def empty: ParameterValues = ParameterValues(Map.empty)

  def fromStringMap(values: Map[String, String] = Map.empty, templates: Map[String, String] = Map.empty): ParameterValues = {
    ParameterValues(values.view.mapValues(ParameterStringValue).toMap) merge ParameterValues(templates.view.mapValues(ParameterTemplateValue).toMap)
  }
}
