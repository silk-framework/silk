package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables, TemplateVariablesManager}

class ProjectTemplateVariablesManager(serializer: TemplateVariablesSerializer)
                                     (implicit user: UserContext) extends TemplateVariablesManager {

  private val projectScope = "project"

  private var variables: TemplateVariables = serializer.readVariables()

  /**
    * The available variable scopes.
    */
  def scopes: Set[String] = GlobalTemplateVariables.scopes + projectScope

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = {
    variables
  }

  /**
    * Updates all template variables.
    */
  override def put(variables: TemplateVariables): Unit = {
    serializer.putVariables(variables)
    this.variables = variables
  }
}
