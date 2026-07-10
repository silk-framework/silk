package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{VariableScope, TemplateVariables, TemplateVariablesManager, TemplateVariablesReader}

/**
 * Manages the execution variables of a task.
 * These provide the default values for the execution scope when the execution of the task is started.
 *
 * @param initialVariables The initial execution variables.
 * @param parentReaders Readers for the parent scope variables (global and project variables).
 */
class TaskExecutionVariablesManager(initialVariables: TemplateVariables,
                                    parentReaders: Seq[TemplateVariablesReader] = Seq.empty) extends TemplateVariablesManager {

  private def executionScope = VariableScope.execution

  // Written under the task's update lock, but read from arbitrary threads (e.g. at workflow run start).
  @volatile
  private var variables: TemplateVariables = initialVariables

  /**
   * The available variable scopes.
   */
  override def scopes: Set[VariableScope] = Set(executionScope)

  /**
    * Returns the global and project variables as the parent scope.
    */
  override def parentVariables: TemplateVariables = {
    parentReaders.map(_.all).foldLeft(TemplateVariables.empty)(_ merge _)
  }

  /**
    * All managed variables must be in the execution scope.
    */
  override def variableScope: VariableScope = executionScope

  /**
   * Retrieves all execution variables.
   */
  override def all: TemplateVariables = variables

  /**
   * Updates all execution variables.
   */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = {
    validateScope(variables)
    this.variables = variables
  }
}
