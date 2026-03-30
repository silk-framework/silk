package org.silkframework.runtime.templating

object TemplateVariableScopes {

  /**
   * Global variables.
   */
  final val global: Seq[String] = Seq("global")

  /**
   * Project variables.
   */
  final val project: Seq[String] = Seq("project")

}
