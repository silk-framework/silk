package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.templating.exceptions.{CannotModifyVariablesUsedByTaskException, CannotUpdateVariablesUsedByTaskException}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables}
import org.silkframework.workspace.Project

case class UpdateVariablesModification(project: Project, updatedVariables: TemplateVariables) extends Modification {

  override def operation: String = s"Updated the following variables ${updatedVariables.variables.map(_.name).mkString("'", "', '", "'")}"

  override protected def updateVariables(currentVariables: TemplateVariables): TemplateVariables = {
    updatedVariables.resolved(GlobalTemplateVariables.all)
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotUpdateVariablesUsedByTaskException(task, cause)
  }
}
