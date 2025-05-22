package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable

case class DeleteVariableModification(project: Project, variableName: String) extends Modification {

  override def operation: String = s"Deleted variable '$variableName'"

  /**
    * Retrieves the variables that use this variable.
    */
  def dependentVariables(): Seq[String] = {
    try {
      updateVariables(project.templateVariables.all)
      Seq.empty
    } catch {
      case ex: CannotDeleteUsedVariableException =>
        ex.dependentVariables
      case _: TemplateVariablesEvaluationException =>
        Seq.empty
      case ex: Throwable =>
        throw ex
    }
  }

  /**
    * Retrieves the tasks that would become invalid by this modification.
    */
  def invalidTasks()(implicit user: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    // Compute current and new project variables
    val currentVariables = project.templateVariables.all
    val newVariables = TemplateVariables(currentVariables.variables.filter(_.name != variableName))

    // Compute all variables including the global variables
    val allCurrentVariables = GlobalTemplateVariables.all merge currentVariables
    val allNewVariables = GlobalTemplateVariables.all merge newVariables

    // Check if the update variables break a task
    val currentContext: PluginContext = PluginContext.fromProject(project)
    val updatedTasks = mutable.Buffer[ProjectTask[_ <: TaskSpec]]()
    for (task <- project.allTasks) yield {
      try {
        hasUpdatedTemplateValues(task.parameters(currentContext), allCurrentVariables, allNewVariables)
      } catch {
        case _: TemplateEvaluationException =>
          // Task update would fail with the modified variables.
          updatedTasks.append(task)
      }
      task
    }
    updatedTasks.toSeq
  }

  override protected def updateVariables(currentVariables: TemplateVariables): TemplateVariables = {
    // Make sure that variable exists
    val variable = project.templateVariables.get(variableName)

    val updatedVariables = TemplateVariables(currentVariables.variables.filter(_.name != variableName))
    try {
      updatedVariables.resolved(GlobalTemplateVariables.all)
    } catch {
      case ex: TemplateVariablesEvaluationException =>
        // Check if the evaluation failed because this variable is used in other variables.
        val dependentVariables =
          ex.issues.collect {
            case TemplateVariableEvaluationException(dependentVar, unboundEx: UnboundVariablesException) if unboundEx.missingVars.contains(variable) =>
              dependentVar.name
          }
        if (dependentVariables.nonEmpty) {
          throw CannotDeleteUsedVariableException(variableName, dependentVariables)
        } else {
          throw ex
        }
    }
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotDeleteVariableUsedByTaskException(variableName, task, cause)
  }
}
