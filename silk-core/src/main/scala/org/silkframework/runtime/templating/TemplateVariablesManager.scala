package org.silkframework.runtime.templating

import org.silkframework.runtime.activity.UserContext

/**
  * Allows to retrieve and edit a set of template variables.
  */
trait TemplateVariablesManager extends TemplateVariablesReader {

  /**
    * Updates all template variables.
    */
  def put(variables: TemplateVariables)(implicit user: UserContext): Unit

}