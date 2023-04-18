package org.silkframework.runtime.templating

/**
  * Allows to retrieve and edit a set of template variables.
  */
trait TemplateVariablesManager extends TemplateVariablesReader {

  /**
    * Add or update a template variable.
    */
  def put(variable: TemplateVariable): Unit

  /**
    * Remove an existing template variable.
    */
  def remove(name: String): Unit

}