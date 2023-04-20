package org.silkframework.runtime.templating

/**
  * Allows to retrieve and edit a set of template variables.
  */
trait TemplateVariablesManager extends TemplateVariablesReader {

  /**
    * Updates all template variables.
    */
  def put(variables: TemplateVariables): Unit

}