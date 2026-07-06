package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.InvalidScopeException
import org.silkframework.runtime.templating.{TemplateVariableScopes, TemplateVariables, TemplateVariablesManager, TemplateVariablesReader}

/**
 * Manages the execution variables of a task.
 * These provide the default values for the execution scope when the execution of the task is started.
 *
 * @param initialVariables The initial execution variables.
 * @param parentReaders Readers for the parent scope variables (global and project variables).
 */
class TaskExecutionVariablesManager(initialVariables: TemplateVariables,
                                    parentReaders: Seq[TemplateVariablesReader] = Seq.empty) extends TemplateVariablesManager {

  private def executionScope = TemplateVariableScopes.execution

  private var variables: TemplateVariables = initialVariables

  /**
   * The available variable scopes.
   */
  override def scopes: Set[Seq[String]] = Set(executionScope)

  /**
    * Returns the global and project variables as the parent scope.
    */
  override def parentVariables: TemplateVariables = {
    parentReaders.map(_.all).foldLeft(TemplateVariables.empty)(_ merge _)
  }

  /**
   * Retrieves all execution variables.
   */
  override def all: TemplateVariables = variables

  /**
   * Updates all execution variables.
   */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    for (variable <- variables.variables) {
      if (variable.scope != executionScope) {
        throw new InvalidScopeException(s"Variable '${variable.name}' has an invalid scope '${variable.scope}'. " +
          s"Currently, only variables in the '$executionScope' scope can be modified.")
      }
    }
    this.variables = variables
  }
}
