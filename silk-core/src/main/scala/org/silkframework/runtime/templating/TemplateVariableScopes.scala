package org.silkframework.runtime.templating

/**
 * Predefined variable scopes. Each scope is a sequence of strings that forms a prefix path
 * used to address variables, e.g., a variable "label" in scope Seq("project") is addressed as "project.label".
 */
object TemplateVariableScopes {

  /**
   * Scope for global variables, addressed as "global.variableName".
   */
  final val global: Seq[String] = Seq("global")

  /**
   * Scope for project variables, addressed as "project.variableName".
   */
  final val project: Seq[String] = Seq("project")

  /**
   * Workflow variables.
   */
  final val workflow = "workflow"

}
