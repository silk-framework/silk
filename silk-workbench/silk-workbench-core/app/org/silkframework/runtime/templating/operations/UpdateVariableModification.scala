package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.templating.exceptions.{CannotModifyVariablesUsedByTaskException, CannotUpdateVariableUsedByTaskException}
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
import org.silkframework.workspace.Project

case class UpdateVariableModification(project: Project, variable: TemplateVariable, taskId: Option[String] = None) extends Modification {

  override def operation: String = s"Updated variable $variable"

  override protected def updateVariables(currentVariables: TemplateVariables, parentVariables: TemplateVariables): TemplateVariables = {
    UpdateVariableModification.updateVariable(currentVariables, variable).resolved(parentVariables)
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotUpdateVariableUsedByTaskException(variable.name, task, cause)
  }
}

object UpdateVariableModification {

  /**
    * Applies a single variable update to an existing set of variables.
    * Appends the variable if it does not exist yet, or replaces it in-place if it does.
    */
  private def updateVariable(currentVariables: TemplateVariables, variable: TemplateVariable): TemplateVariables = {
    val variables = currentVariables.variables
    variables.indexWhere(_.name == variable.name) match {
      case -1 =>
        TemplateVariables(variables :+ variable)
      case index: Int =>
        TemplateVariables(variables.updated(index, variable))
    }
  }
}
