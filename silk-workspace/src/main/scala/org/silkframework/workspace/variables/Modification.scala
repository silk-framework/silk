package org.silkframework.workspace.variables

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterTemplateValue, ParameterValues, PluginContext, TaskResolver}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{GlobalTemplateVariables, InMemoryTemplateVariablesReader, TemplateVariableName, VariableScope, TemplateVariables, TemplateVariablesManager}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NonFatal

/**
  * Modifies either project variables or the execution variables of a task.
  * For project variables, also updates all tasks that use variables that have been modified.
  */
abstract class Modification {

  private val log: Logger = Logger.getLogger(getClass.getName)

  /**
    * The project whose variables are to be modified.
    */
  def project: Project

  /**
    * Optional task identifier. If set, the modification operates on the execution variables of that task instead of project variables.
    */
  def taskId: Option[String]

  /**
    * Brief description of the done operation, e.g., "Deleted variables".
    */
  def operation: String

  /**
    * Implements the concrete variables modification.
    *
    * @param currentVariables The current variables at the target scope.
    * @param parentVariables The resolved parent scope variables (without sensitive values) available for template resolution.
    */
  protected def updateVariables(currentVariables: TemplateVariables, parentVariables: TemplateVariables)
                               (implicit user: UserContext): TemplateVariables

  /**
    * Generates an exception if a variable could not be updated, because a task would become invalid.
    */
  protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException

  /**
    * The manager of the variables that are modified by this operation.
    */
  protected def variablesManager()(implicit user: UserContext): TemplateVariablesManager = {
    project.variablesManager(taskId)
  }

  /**
    * Updates variables and persists the changes.
    * For project variables, also updates all tasks that use modified variables.
    * For the execution variables of a task, updates and persists them on the task.
    */
  def execute()(implicit user: UserContext): Unit = {
    val manager = variablesManager()
    // Read and write are one step under the monitor that `put` takes, so concurrent modifications do not overwrite each other.
    manager.synchronized {
      val currentVariables = manager.all
      val newVariables = updateVariables(currentVariables, manager.parentVariables.withoutSensitiveVariables())
      taskId match {
        case Some(id) =>
          updateExecutionVariablesAndTask(project.anyTask(id), currentVariables, newVariables)
          log.info(s"$operation.")
        case None =>
          // The scope check is the one expected failure of the put, so it precedes the task updates.
          manager.validateScope(newVariables)
          // The task updates follow the variables and are restored by reverting the variable change, so they are not journaled.
          val updatedTaskIds = project.changeJournal.derived(updateTasks(newVariables))
          manager.put(newVariables)
          // Execution-variable templates are resolved at save time, so re-resolve them against the updated variables.
          val refreshedTaskIds = project.changeJournal.derived(refreshTaskExecutionVariables())
          val allUpdatedIds = (updatedTaskIds ++ refreshedTaskIds).toSeq.distinct
          if(allUpdatedIds.nonEmpty) {
            log.info(s"$operation. The following tasks have been updated: " + allUpdatedIds)
          } else {
            log.info(s"$operation. No tasks have been updated.")
          }
      }
    }
  }

  /**
    * Updates the execution variables of a task.
    * If any of the task's parameter templates resolves to a different value under the new variables,
    * the task itself is re-instantiated and persisted together with the variables.
    */
  private def updateExecutionVariablesAndTask(projectTask: ProjectTask[_ <: TaskSpec],
                                              currentVariables: TemplateVariables,
                                              newVariables: TemplateVariables)(implicit user: UserContext): Unit = {
    val baseVariables = project.combinedTemplateVariables.all
    val currentContext = PluginContext.fromTask(projectTask, project)
    val updatedData =
      try {
        val parameters = projectTask.parameters(currentContext)
        if (hasUpdatedTemplateValues(parameters, baseVariables merge currentVariables, baseVariables merge newVariables)) {
          val newContext = currentContext.copy(templateVariables = currentContext.templateVariables.withExecutionDefaults(newVariables))
          Some(projectTask.withParameters(parameters, dropExistingValues = true)(newContext))
        } else {
          None
        }
      } catch {
        case NonFatal(ex) =>
          throw generateException(projectTask, ex)
      }
    updatedData match {
      case Some(newData) =>
        project.updateAnyTask(projectTask.id, newData, executionVariables = Some(newVariables))
      case None =>
        projectTask.updateExecutionVariables(newVariables)
    }
  }

  /** Re-resolves the execution-variable templates of all tasks and persists changed values. */
  private def refreshTaskExecutionVariables()(implicit user: UserContext): Seq[Identifier] = {
    for {
      task <- project.allTasks.toSeq
      if task.executionVariables.variables.exists(_.template.isDefined)
      resolved = task.executionVariables.resolvedKeepingUnresolved(
        task.executionVariablesValueHolder.parentVariables.withoutSensitiveVariables())
      if resolved != task.executionVariables
    } yield {
      updateExecutionVariablesAndTask(task, task.executionVariables, resolved)
      task.id
    }
  }

  private def updateTasks(newVariables: TemplateVariables)(implicit user: UserContext): Iterable[Identifier] = {
    val allCurrentVariables = project.combinedTemplateVariables.all
    val allNewVariables = GlobalTemplateVariables.all merge newVariables

    val currentContext: PluginContext = PluginContext.fromProject(project)
    val newContext =
      PluginContext(prefixes = project.config.prefixes,
        resources = project.resources,
        user = user,
        projectId = Some(project.config.id),
        templateVariables = InMemoryTemplateVariablesReader(allNewVariables, currentContext.templateVariables.scopes),
        taskResolver = TaskResolver.fromProject(project)
      )

    val updatedTasks = mutable.Buffer[(Identifier, TaskSpec)]()
    for (task <- project.allTasks) {
      try {
        if (hasUpdatedTemplateValues(task, currentContext, allCurrentVariables, allNewVariables)) {
          val taskNewContext = newContext.copy(templateVariables = newContext.templateVariables.withExecutionDefaults(task.executionVariables))
          updatedTasks.append((task.id, task.withParameters(task.parameters(currentContext), dropExistingValues = true)(taskNewContext)))
        }
      } catch {
        case NonFatal(ex) =>
          throw generateException(task, ex)
      }
    }

    for ((id, updatedTask) <- updatedTasks) {
      project.updateAnyTask(id, updatedTask)
    }

    updatedTasks.map(_._1)
  }

  /**
    * Throws via [[generateException]] if a removed project variable is still referenced by a task,
    * either from an execution-variable template or from a template within the task data.
    */
  protected def checkRemovedVariableDependencies(newProjectVariables: TemplateVariables,
                                                 removedVariableNames: Set[String])
                                                (implicit user: UserContext): Unit = {
    if (taskId.isEmpty && removedVariableNames.nonEmpty) {
      for ((task, cause) <- tasksWithDependentExecutionVariables(newProjectVariables, removedVariableNames).headOption) {
        throw generateException(task, cause)
      }
      for (task <- project.allTasks) {
        val referenced = referencedRemovedVariables(task, removedVariableNames)
        if (referenced.nonEmpty) {
          throw generateException(task, new ValidationException(
            s"The task references the variable(s) ${referenced.map(_.scopedName).mkString("'", "', '", "'")} from a template, e.g., in an 'Evaluate template' operator."))
        }
      }
    }
  }

  /** The removed project variables that the task data reports as referenced, e.g., from an 'Evaluate template' operator. */
  protected def referencedRemovedVariables(task: ProjectTask[_ <: TaskSpec],
                                           removedVariableNames: Set[String]): Seq[TemplateVariableName] = {
    task.data.referencedVariables.filter(v => v.scope == VariableScope.project && removedVariableNames.contains(v.name)).distinct
  }

  /**
    * Tasks whose execution-variable templates reference one of the removed project variables.
    */
  protected def tasksWithDependentExecutionVariables(newProjectVariables: TemplateVariables,
                                                     removedVariableNames: Set[String])
                                                    (implicit user: UserContext): Seq[(ProjectTask[_ <: TaskSpec], TemplateVariablesEvaluationException)] = {
    // Match the resolution at save time (parent scopes without sensitive variables).
    val parentVariables = (GlobalTemplateVariables.all merge newProjectVariables).withoutSensitiveVariables()
    for (task <- project.allTasks.toSeq;
         issues = dependentExecutionVariableIssues(task, parentVariables, removedVariableNames) if issues.nonEmpty) yield {
      (task, TemplateVariablesEvaluationException(issues))
    }
  }

  /** The evaluation issues of a task's execution variables that are caused by the removed variables. */
  protected def dependentExecutionVariableIssues(task: ProjectTask[_ <: TaskSpec],
                                               parentVariables: TemplateVariables,
                                               removedVariableNames: Set[String]): Seq[TemplateVariableEvaluationException] = {
    try {
      task.executionVariables.resolved(parentVariables)
      Seq.empty
    } catch {
      case ex: TemplateVariablesEvaluationException =>
        // Failures unrelated to the removed variables do not block the modification.
        ex.issues.filter {
          case TemplateVariableEvaluationException(_, unboundEx: UnboundVariablesException) =>
            unboundEx.missingVars.exists(missing =>
              removedVariableNames.contains(missing.name) && missing.scope == VariableScope.project)
          case _ =>
            false
        }
    }
  }

  /**
    * Checks whether any of a task's parameter templates evaluates to a different value under the new variables.
    * The task's own execution-variable defaults are included in both variable sets, so that parameter templates
    * referencing 'execution.X' can be evaluated.
    */
  protected def hasUpdatedTemplateValues(task: Task[_ <: TaskSpec], context: PluginContext,
                                         currentVariables: TemplateVariables, newVariables: TemplateVariables): Boolean = {
    hasUpdatedTemplateValues(task.parameters(context),
      currentVariables merge task.executionVariables,
      newVariables merge task.executionVariables)
  }

  protected def hasUpdatedTemplateValues(parameters: ParameterValues, currentVariables: TemplateVariables, newVariables: TemplateVariables): Boolean = {
    var updated = false
    breakable {
      for (parameters <- parameters.values.values) {
        updated = parameters match {
          case template: ParameterTemplateValue =>
            template.evaluate(currentVariables) != template.evaluate(newVariables)
          case childParameters: ParameterValues =>
            hasUpdatedTemplateValues(childParameters, currentVariables, newVariables)
          case _ =>
            false
        }
        if (updated) {
          break()
        }
      }
    }
    updated
  }

}






