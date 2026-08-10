package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, VariableScope, TemplateVariables, TemplateVariablesManager}

/**
 * Manages project template variables.
 *
 * @param serializer The serializer to read and write template variables.
 * @param loadingUser The user context for loading the variables initially.
 */
class ProjectTemplateVariablesManager(serializer: TemplateVariablesSerializer, loadingUser: UserContext) extends TemplateVariablesManager {

  private def projectScope = VariableScope.project

  @volatile
  private var variables: TemplateVariables = serializer.readVariables()(loadingUser)

  /**
    * The available variable scopes.
    */
  def scopes: Set[VariableScope] = GlobalTemplateVariables.scopes + projectScope

  /**
    * Returns the global variables as the parent scope.
    */
  override def parentVariables: TemplateVariables = GlobalTemplateVariables.all

  /**
    * All managed variables must be in the project scope.
    */
  override def variableScope: VariableScope = projectScope

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = {
    variables
  }

  /**
    * Updates all template variables.
    */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    validateScope(variables)
    serializer.putVariables(variables)
    this.variables = variables
  }
}
