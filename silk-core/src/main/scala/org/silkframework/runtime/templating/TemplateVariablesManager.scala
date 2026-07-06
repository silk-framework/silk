package org.silkframework.runtime.templating

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.InvalidScopeException

/**
  * Allows to retrieve and edit a set of template variables.
  */
trait TemplateVariablesManager extends TemplateVariablesReader {

  /**
    * Returns the parent scope variables that can be referenced in templates.
    * For project variables, this returns the global variables.
    * For the execution variables of a task, this returns the global and project variables.
    */
  def parentVariables: TemplateVariables

  /**
    * The scope that all variables managed by this manager must be in.
    */
  def variableScope: Seq[String]

  /**
    * Validates that all given variables are in this manager's scope.
    *
    * @throws InvalidScopeException If a variable has a different scope.
    */
  def validateScope(variables: TemplateVariables): Unit = {
    for (variable <- variables.variables) {
      if (variable.scope != variableScope) {
        throw new InvalidScopeException(s"Variable '${variable.name}' has an invalid scope '${variable.scope.mkString(".")}'. " +
          s"Currently, only variables in the '${variableScope.mkString(".")}' scope can be modified.")
      }
    }
  }

  /**
    * Updates all template variables.
    */
  def put(variables: TemplateVariables)(implicit user: UserContext): Unit

}