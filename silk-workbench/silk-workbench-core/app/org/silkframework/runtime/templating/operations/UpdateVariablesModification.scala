package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.TemplateVariables
import org.silkframework.runtime.templating.exceptions.{CannotModifyVariablesUsedByTaskException, CannotUpdateVariablesUsedByTaskException}
import org.silkframework.workspace.Project

case class UpdateVariablesModification(project: Project, updatedVariables: TemplateVariables, taskId: Option[String] = None) extends Modification {

  override def operation: String = s"Updated the following variables ${updatedVariables.variables.map(_.name).mkString("'", "', '", "'")}"

  override protected def updateVariables(currentVariables: TemplateVariables, parentVariables: TemplateVariables)
                                        (implicit user: UserContext): TemplateVariables = {
    val resolvedVariables = updatedVariables.resolved(parentVariables)
    // Replacing the variable set may remove variables that tasks still reference.
    val removedVariableNames = currentVariables.variables.map(_.name).toSet -- updatedVariables.variables.map(_.name).toSet
    checkRemovedVariableDependencies(resolvedVariables, removedVariableNames)
    resolvedVariables
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotUpdateVariablesUsedByTaskException(task, cause)
  }
}
