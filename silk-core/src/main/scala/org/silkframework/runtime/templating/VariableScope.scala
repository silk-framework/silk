package org.silkframework.runtime.templating

/**
  * The scope of a template variable: a prefix path that is prepended to the variable name when addressing it,
  * e.g., a variable "label" in scope VariableScope("project") is addressed as "project.label".
  *
  * @param path The scope path segments. May be empty for top-level variables.
  */
case class VariableScope(path: Seq[String]) {

  /** True, if this is the empty (top-level) scope. */
  def isEmpty: Boolean = path.isEmpty

  def nonEmpty: Boolean = path.nonEmpty

  /** Extends this scope by a nested segment. */
  def /(segment: String): VariableScope = VariableScope(path :+ segment)

  /** Dot-separated representation, e.g., "project.metaData". Empty string for the empty scope. */
  override def toString: String = path.mkString(".")
}

object VariableScope {

  final val empty: VariableScope = VariableScope(Seq.empty)

  /** Scope for global variables, addressed as "global.variableName". */
  final val global: VariableScope = VariableScope("global")

  /** Scope for project variables, addressed as "project.variableName". */
  final val project: VariableScope = VariableScope("project")

  /** Scope for execution variables, addressed as "execution.variableName". */
  final val execution: VariableScope = VariableScope("execution")

  /** All predefined scopes that variables can be defined in. */
  final val all: Seq[VariableScope] = Seq(global, project, execution)

  /** Convenience for the common single-segment scope. */
  def apply(segment: String): VariableScope = VariableScope(Seq(segment))

  /** Parses a dot-separated scope string, e.g., "project.metaData". Empty segments are dropped. */
  def parse(str: String): VariableScope = VariableScope(str.split('.').filter(_.nonEmpty).toSeq)
}
