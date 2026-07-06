package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable

case class DeleteVariableModification(project: Project, variableName: String, taskId: Option[String] = None) extends Modification {

  override def operation: String = s"Deleted variable '$variableName'"

  /**
    * Retrieves the variables that use this variable.
    */
  def dependentVariables()(implicit user: UserContext): Seq[String] = {
    val manager = variablesManager()
    try {
      // Resolve against the same (sensitive-filtered) parent scope as the actual delete in Modification.execute.
      updateVariables(manager.all, manager.parentVariables.withoutSensitiveVariables())
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
    taskId match {
      case Some(id) =>
        // Execution variables can only be referenced by parameter templates of the task itself.
        val task = project.anyTask(id)
        val currentVariables = task.executionVariables
        val newVariables = TemplateVariables(currentVariables.variables.filter(_.name != variableName))
        val baseVariables = project.combinedTemplateVariables.all
        val context = PluginContext.fromTask(task, project)
        try {
          hasUpdatedTemplateValues(task.parameters(context), baseVariables merge currentVariables, baseVariables merge newVariables)
          Seq.empty
        } catch {
          case _: TemplateEvaluationException =>
            // Task update would fail with the modified variables.
            Seq(task)
        }
      case None =>
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
            hasUpdatedTemplateValues(task, currentContext, allCurrentVariables, allNewVariables)
          } catch {
            case _: TemplateEvaluationException =>
              // Task update would fail with the modified variables.
              updatedTasks.append(task)
          }
          task
        }
        updatedTasks.toSeq
    }
  }

  override protected def updateVariables(currentVariables: TemplateVariables, parentVariables: TemplateVariables): TemplateVariables = {
    // Make sure that variable exists
    val variable = currentVariables.map.getOrElse(variableName,
      throw new org.silkframework.runtime.validation.NotFoundException(s"No variable '$variableName' has been found."))

    val updatedVariables = TemplateVariables(currentVariables.variables.filter(_.name != variableName))
    try {
      updatedVariables.resolved(parentVariables)
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
          // The remaining failures are unrelated to the deleted variable (e.g. templates referencing
          // a sensitive parent variable, which is not available for template resolution).
          // Those variables keep their stored values, so that unrelated variables can still be deleted.
          updatedVariables.resolvedKeepingUnresolved(parentVariables)
        }
    }
  }

  override protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException = {
    CannotDeleteVariableUsedByTaskException(variableName, task, cause)
  }
}
