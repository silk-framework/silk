package org.silkframework.runtime.templating

/**
  * Template variables available to a plugin during creation, update, or execution.
  *
  * Combines a set of immutable parent readers (global, project, task scopes) with a shared, mutable
  * [[ExecutionVariablesHolder]] for the execution scope. The global, project, and task scopes are
  * immutable for the lifetime of a single plugin invocation; only the execution scope is mutable, via
  * [[setExecutionVariable]].
  *
  * The immutable parent scopes never change for the lifetime of this instance, so their merged snapshot
  * and the derived execution-scope fallback candidates are computed once (lazily). Only the holder
  * snapshot is re-read on each `all` call so that execution-scope mutations stay visible. During a
  * workflow run all contexts share the same holder, so a mutation by one task is visible to subsequent
  * tasks. Outside a workflow run the context carries its own (initially empty) holder, so setting an
  * execution variable is local to that context.
  *
  * The execution scope falls back to the parent scopes: when a variable is addressed as `execution.X`
  * but `X` has not been set directly in the execution scope, it resolves to the `task`, then `project`,
  * then `global` variable of the same name (task has the highest precedence). A value set directly in
  * the execution scope always wins and suppresses the fallback. See [[fallbackCandidates]].
  */
final case class ExecutionTemplateVariables(immutableScopes: Seq[TemplateVariablesReader],
                                            holder: ExecutionVariablesHolder = new ExecutionVariablesHolder())
    extends TemplateVariablesReader {

  override def scopes: Set[Seq[String]] =
    immutableScopes.flatMap(_.scopes).toSet + TemplateVariableScopes.execution

  /** Merged snapshot of the immutable parent scopes (global, project, task). Computed once. */
  private lazy val immutableVariables: TemplateVariables =
    immutableScopes.map(_.all).reduceOption(_ merge _).getOrElse(TemplateVariables.empty)

  /**
    * Execution-scope fallback candidates, keyed by variable name. For each name defined in the task,
    * project or global scope, holds a single copy re-scoped to `execution` using the highest-precedence
    * source (task > project > global). Sensitivity and templates of the source variable are preserved.
    *
    * This only depends on the immutable parent scopes, so it is computed once. The (mutable) execution
    * holder is applied per call in [[all]], which removes any candidate that has been set directly in
    * the execution scope, so that direct values always shadow the fallback.
    */
  private lazy val fallbackCandidates: Map[String, TemplateVariable] = {
    // Precedence by exact scope: task > project > global (smaller wins). Other scopes do not fall back.
    def precedence(scope: Seq[String]): Option[Int] = scope match {
      case TemplateVariableScopes.task    => Some(0)
      case TemplateVariableScopes.project => Some(1)
      case TemplateVariableScopes.global  => Some(2)
      case _                              => None
    }

    immutableVariables.variables
      .flatMap(v => precedence(v.scope).map(p => (v, p)))
      .groupBy { case (v, _) => v.name }
      .map { case (name, candidates) =>
        name -> candidates.minBy { case (_, p) => p }._1.copy(scope = TemplateVariableScopes.execution)
      }
  }

  override def all: TemplateVariables = {
    val executionVars = holder.snapshot
    val executionNames = executionVars.variables.map(_.name).toSet
    val fallbacks = fallbackCandidates.filterNot { case (name, _) => executionNames.contains(name) }.values.toSeq
    immutableVariables merge executionVars merge TemplateVariables(fallbacks)
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
