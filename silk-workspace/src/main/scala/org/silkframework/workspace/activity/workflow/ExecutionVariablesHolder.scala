package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.templating.exceptions.InvalidScopeException
import org.silkframework.runtime.templating.{PluginTemplateVariables, TemplateVariable, TemplateVariableScopes, TemplateVariables, TemplateVariablesReader}

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
    * @throws InvalidScopeException if the variable's scope is not [[TemplateVariableScopes.execution]].
    */
  def set(variable: TemplateVariable): Unit = {
    if (variable.scope != TemplateVariableScopes.execution) {
      throw new InvalidScopeException(
        s"Variable '${variable.name}' has scope '${variable.scope.mkString(".")}'. " +
        s"Only variables in the '${TemplateVariableScopes.execution.mkString(".")}' scope can be modified through PluginContext.")
    }
    ref.updateAndGet { current =>
      TemplateVariables(variable +: current.variables.filterNot(_.name == variable.name))
    }
  }
}

/**
  * A [[PluginTemplateVariables]] used during workflow execution.
  * Combines a set of immutable parent readers (global, project, task scopes) with a shared
  * mutable [[ExecutionVariablesHolder]] for the execution scope.
  *
  * Unlike [[org.silkframework.runtime.templating.CombinedTemplateVariablesReader]], `all` is NOT
  * cached — it re-reads the holder snapshot on each call so that mutations are reflected.
  */
final case class WorkflowExecutionPluginTemplateVariables(immutableScopes: Seq[TemplateVariablesReader],
                                                          holder: ExecutionVariablesHolder) extends PluginTemplateVariables {

  override def scopes: Set[Seq[String]] =
    immutableScopes.flatMap(_.scopes).toSet + TemplateVariableScopes.execution

  override def all: TemplateVariables = {
    val readerSnapshots = immutableScopes.map(_.all)
    (readerSnapshots :+ holder.snapshot).reduceOption(_ merge _).getOrElse(TemplateVariables.empty)
  }

  override def setExecutionVariable(variable: TemplateVariable): Unit = holder.set(variable)
}
