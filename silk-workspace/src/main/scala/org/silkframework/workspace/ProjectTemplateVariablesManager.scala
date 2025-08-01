package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.InvalidScopeException
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariableScopes, TemplateVariables, TemplateVariablesManager}

/**
 * Manages project template variables.
 *
 * @param serializer The serializer to read and write template variables.
 * @param loadingUser The user context for loading the variables initially.
 */
class ProjectTemplateVariablesManager(serializer: TemplateVariablesSerializer, loadingUser: UserContext) extends TemplateVariablesManager {

  private def projectScope = TemplateVariableScopes.project

  private var variables: TemplateVariables = serializer.readVariables()(loadingUser)

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
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    // Make sure that all variables are in the project scope.
    for(variable <- variables.variables) {
      if(variable.scope != projectScope) {
        throw new InvalidScopeException(s"Variable '${variable.name}' has an invalid scope '${variable.scope}'. " +
          s"Currently, only variables in the '$projectScope' scope can be modified.")
      }
    }
    serializer.putVariables(variables)
    this.variables = variables
  }
}
