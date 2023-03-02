package org.silkframework.runtime.templating

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
case class TemplateVariables(map: Map[String, String])

object TemplateVariables {

  def empty: TemplateVariables = TemplateVariables(Map.empty)

}
