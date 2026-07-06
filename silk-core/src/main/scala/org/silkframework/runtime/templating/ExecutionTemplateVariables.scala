package org.silkframework.runtime.templating

/**
  * Template variables available to a plugin during creation, update, or execution.
  *
  * Combines a set of immutable parent readers (global and project scopes) with a shared, mutable
  * [[ExecutionVariablesHolder]] for the execution scope. The global and project scopes are immutable
  * for the lifetime of a single plugin invocation; only the execution scope is mutable, via
  * [[setExecutionVariable]].
  *
  * The execution scope is independent of the other scopes: a reference to `execution.X` resolves only
  * if `X` has been set in the execution scope (seeded from the executed task's execution variables,
  * overridden when the execution is started, or written during the execution). During a workflow run
  * all contexts share the same holder, so a mutation by one task is visible to subsequent tasks.
  * Outside a workflow run the context carries its own (initially empty) holder, so setting an
  * execution variable is local to that context.
  */
final case class ExecutionTemplateVariables(immutableScopes: Seq[TemplateVariablesReader],
                                            holder: ExecutionVariablesHolder = new ExecutionVariablesHolder())
    extends TemplateVariablesReader {

  override def scopes: Set[Seq[String]] =
    immutableScopes.flatMap(_.scopes).toSet + TemplateVariableScopes.execution

  /** Merged snapshot of the immutable parent scopes (global, project). Computed once. */
  private lazy val immutableVariables: TemplateVariables =
    immutableScopes.map(_.all).reduceOption(_ merge _).getOrElse(TemplateVariables.empty)

  override def all: TemplateVariables = {
    immutableVariables merge holder.snapshot
  }

  /**
    * Sets (creates or replaces) a single variable in the execution scope.
    * Replace-on-collision by name; other scopes are not affected.
    *
    * @param variable Must have `scope == TemplateVariableScopes.execution`.
    * @throws org.silkframework.runtime.templating.exceptions.InvalidScopeException if `variable.scope` is not the execution scope.
    */
  def setExecutionVariable(variable: TemplateVariable): Unit = holder.set(variable)
}

object ExecutionTemplateVariables {

  /**
    * Wraps a [[TemplateVariablesReader]] as an [[ExecutionTemplateVariables]]. If the reader is already an
    * [[ExecutionTemplateVariables]], it is returned unchanged so that the holder is preserved when threading
    * a context through.
    */
  def apply(reader: TemplateVariablesReader): ExecutionTemplateVariables = reader match {
    case etv: ExecutionTemplateVariables => etv
    case other => ExecutionTemplateVariables(Seq(other))
  }
}
