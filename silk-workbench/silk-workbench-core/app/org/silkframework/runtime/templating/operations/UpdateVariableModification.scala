package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.templating.exceptions.{CannotModifyVariablesUsedByTaskException, CannotUpdateVariableUsedByTaskException}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariables}
import org.silkframework.workspace.Project

case class UpdateVariableModification(project: Project, variable: TemplateVariable) extends Modification {

  override def operation: String = s"Updated variable $variable"

  override protected def updateVariables(currentVariables: TemplateVariables): TemplateVariables = {
    val variables = currentVariables.variables
    val updatedVariables = variables.indexWhere(_.name == variable.name) match {
      case -1 =>
        TemplateVariables(variables :+ variable)
      case index: Int =>
        TemplateVariables(variables.updated(index, variable))
    }
    updatedVariables.resolved(GlobalTemplateVariables.all.withoutSensitiveVariables())
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotUpdateVariableUsedByTaskException(variable.name, task, cause)
  }
}
