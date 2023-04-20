package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables, TemplateVariablesManager}

class ProjectTemplateVariablesManager(serializer: TemplateVariablesSerializer) extends TemplateVariablesManager {

  private val projectScope = "project"

  /**
    * The available variable scopes.
    */
  def scopes: Set[String] = GlobalTemplateVariables.scopes + projectScope

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = GlobalTemplateVariables.all

  /**
    * Updates all template variables.
    */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    serializer.putVariables(variables)
  }
}
