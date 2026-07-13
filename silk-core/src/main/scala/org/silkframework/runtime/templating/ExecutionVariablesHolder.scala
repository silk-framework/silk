package org.silkframework.runtime.templating

import org.silkframework.runtime.templating.exceptions.InvalidScopeException

import java.util.concurrent.atomic.AtomicReference

/**
  * Shared, thread-safe holder for the execution-scope template variables of a single workflow run.
  *
  * All [[org.silkframework.runtime.plugin.PluginContext]]s constructed during a workflow run point at the
  * same holder, so a mutation by one task is visible to subsequent tasks.
  */
final class ExecutionVariablesHolder(initial: TemplateVariables = TemplateVariables.empty) {

  private val ref = new AtomicReference[TemplateVariables](initial)

  /** Returns an immutable snapshot of the current execution variables. */
  def snapshot: TemplateVariables = ref.get()

  /**
    * Sets or replaces an execution-scope variable. Replace-on-collision by name.
    *
    * @throws InvalidScopeException if the variable's scope is not [[VariableScope.execution]].
    */
  def set(variable: TemplateVariable): Unit = {
    if (variable.scope != VariableScope.execution) {
      throw new InvalidScopeException(
        s"Variable '${variable.name}' has scope '${variable.scope}'. " +
        s"Only variables in the '${VariableScope.execution}' scope can be modified through PluginContext.")
    }
    ref.updateAndGet { current =>
      TemplateVariables(variable +: current.variables.filterNot(_.name == variable.name))
    }
  }
}
