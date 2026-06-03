package org.silkframework.runtime.templating

import org.silkframework.runtime.templating.exceptions.InvalidScopeException

/**
  * Template variables available to a plugin during creation, update, or execution.
  *
  * Combines a [[TemplateVariablesReader]] over all scopes (global, project, task, execution) with
  * the ability to set variables in the *execution* scope. The global, project, and task scopes are
  * immutable for the lifetime of a single plugin invocation. Only the execution scope is mutable,
  * and only when the underlying context backs a workflow execution.
  *
  * In contexts that do not back a workflow run (e.g. [[org.silkframework.runtime.plugin.PluginContext.empty]],
  * plugin description loading, serialization contexts outside a workflow), [[setExecutionVariable]]
  * throws [[UnsupportedOperationException]].
  */
trait PluginTemplateVariables extends TemplateVariablesReader {

  /**
    * Sets (creates or replaces) a single variable in the execution scope.
    *
    * Replace-on-collision: if a variable with the same name already exists in the execution scope,
    * it is overwritten. Other scopes are not affected.
    *
    * @param variable Must have `scope == TemplateVariableScopes.execution`.
    * @throws UnsupportedOperationException if this context does not back a workflow execution.
    * @throws InvalidScopeException         if `variable.scope` is not the execution scope.
    */
  def setExecutionVariable(variable: TemplateVariable): Unit
}

object PluginTemplateVariables {

  /**
    * Wraps a [[TemplateVariablesReader]] as a [[PluginTemplateVariables]] without mutation support.
    * If the reader is already a [[PluginTemplateVariables]], it is returned unchanged so that
    * mutability is preserved when threading a context through.
    */
  def apply(reader: TemplateVariablesReader): PluginTemplateVariables = reader match {
    case ptv: PluginTemplateVariables => ptv
    case other => ReadOnlyPluginTemplateVariables(other)
  }
}

/**
  * A [[PluginTemplateVariables]] that does not support execution-scope mutation.
  * Used by every [[org.silkframework.runtime.plugin.PluginContext]] that is not backing a workflow execution.
  */
final case class ReadOnlyPluginTemplateVariables(underlying: TemplateVariablesReader) extends PluginTemplateVariables {

  override def scopes: Set[Seq[String]] = underlying.scopes

  override def all: TemplateVariables = underlying.all

  override def setExecutionVariable(variable: TemplateVariable): Unit = {
    throw new UnsupportedOperationException(
      "This PluginContext does not support setting execution variables. Execution variables can only be set during a workflow execution.")
  }
}
