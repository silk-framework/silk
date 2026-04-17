package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.InvalidScopeException
import org.silkframework.runtime.templating.{TemplateVariableScopes, TemplateVariables, TemplateVariablesManager, TemplateVariablesReader}

/**
 * Manages task template variables.
 *
 * @param initialVariables The initial template variables.
 * @param parentReaders Readers for the parent scope variables (global and project variables).
 */
class TaskTemplateVariablesManager(initialVariables: TemplateVariables,
                                   parentReaders: Seq[TemplateVariablesReader] = Seq.empty) extends TemplateVariablesManager {

  private def taskScope = TemplateVariableScopes.task

  private var variables: TemplateVariables = initialVariables

  /**
   * The available variable scopes.
   */
  override def scopes: Set[Seq[String]] = Set(taskScope)

  /**
    * Returns the global and project variables as the parent scope.
    */
  override def parentVariables: TemplateVariables = {
    parentReaders.map(_.all).foldLeft(TemplateVariables.empty)(_ merge _)
  }

  /**
   * Retrieves all template variables.
   */
  override def all: TemplateVariables = variables

  /**
   * Updates all template variables.
   */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    for (variable <- variables.variables) {
      if (variable.scope != taskScope) {
        throw new InvalidScopeException(s"Variable '${variable.name}' has an invalid scope '${variable.scope}'. " +
          s"Currently, only variables in the '$taskScope' scope can be modified.")
      }
    }
    this.variables = variables
  }
}
