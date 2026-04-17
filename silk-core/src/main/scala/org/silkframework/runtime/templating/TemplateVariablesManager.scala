package org.silkframework.runtime.templating

import org.silkframework.runtime.activity.UserContext

/**
  * Allows to retrieve and edit a set of template variables.
  */
trait TemplateVariablesManager extends TemplateVariablesReader {

  /**
    * Returns the parent scope variables that can be referenced in templates.
    * For project variables, this returns the global variables.
    * For task variables, this returns the global and project variables.
    */
  def parentVariables: TemplateVariables

  /**
    * Updates all template variables.
    */
  def put(variables: TemplateVariables)(implicit user: UserContext): Unit

}