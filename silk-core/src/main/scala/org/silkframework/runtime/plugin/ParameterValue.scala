package org.silkframework.runtime.plugin

import org.silkframework.runtime.templating.GlobalTemplateVariables

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
  */
case class ParameterObjectValue(value: AnyRef) extends ParameterValue

/**
  * A template parameter value.
  */
case class ParameterTemplateValue(template: String) extends ParameterValue {

  def evaluate(): String = {
    GlobalTemplateVariables.resolveParameter(template)
  }

}

/**
  * Holds nested parameter values.
  */
case class ParameterValues(values: Map[String, ParameterValue]) extends ParameterValue {

  def toStringMap: Map[String, String] = {
    values.collect {
      case (key, ParameterStringValue(value)) =>
        (key, value)
      case (key, template: ParameterTemplateValue) =>
        (key, template.evaluate())
    }
  }

  def templates: Map[String, String] = {
    values.collect { case (key, t: ParameterTemplateValue) => (key, t.template)}
  }

  def merge(other: ParameterValues): ParameterValues = {
    ParameterValues(
      for((key, value) <- values) yield {
        val mergedValue = other.values.get(key) match {
          case Some(otherNestedValues: ParameterValues) =>
            value match {
              case nestedValues: ParameterValues =>
                nestedValues.merge(otherNestedValues)
              case _ =>
                otherNestedValues
            }
          case Some(otherValue) =>
            otherValue
          case None =>
            value
        }
        (key, mergedValue)
      }
    )
  }

}

object ParameterValues {

  def empty: ParameterValues = ParameterValues(Map.empty)

  def fromStringMap(values: Map[String, String] = Map.empty, templates: Map[String, String] = Map.empty): ParameterValues = {
    ParameterValues(values.mapValues(ParameterStringValue)) merge ParameterValues(templates.mapValues(ParameterTemplateValue))
  }
}
