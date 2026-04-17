package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterTemplateValue, ParameterValues, PluginContext}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{GlobalTemplateVariables, InMemoryTemplateVariablesReader, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NonFatal

/**
  * Modifies variables at either the project or task scope.
  * For project scope, also updates all tasks that use variables that have been modified.
  */
abstract class Modification {

  private val log: Logger = Logger.getLogger(getClass.getName)

  /**
    * The project whose variables are to be modified.
    */
  def project: Project

  /**
    * Optional task identifier. If set, the modification operates on task variables instead of project variables.
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
  protected def updateVariables(currentVariables: TemplateVariables, parentVariables: TemplateVariables): TemplateVariables

  /**
    * Generates an exception if a variable could not be updated, because a task would become invalid.
    */
  protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException

  /**
    * Updates variables and persists the changes.
    * For project scope, also updates all tasks that use modified variables.
    * For task scope, updates and persists the task variables.
    */
  def execute()(implicit user: UserContext): Unit = {
    taskId match {
      case Some(id) =>
        val projectTask = project.anyTask(id)
        val manager = projectTask.variablesValueHolder
        val currentVariables = manager.all
        val newVariables = updateVariables(currentVariables, manager.parentVariables.withoutSensitiveVariables())
        projectTask.updateVariables(newVariables)
        log.info(s"$operation.")
      case None =>
        val manager = project.templateVariables
        val currentVariables = manager.all
        val newVariables = updateVariables(currentVariables, manager.parentVariables.withoutSensitiveVariables())
        val updatedTaskIds = updateTasks(newVariables)
        manager.put(newVariables)
        if(updatedTaskIds.nonEmpty) {
          log.info(s"$operation. The following tasks have been updated: " + updatedTaskIds)
        } else {
          log.info(s"$operation. No tasks have been updated.")
        }
    }
  }

  private def updateTasks(newVariables: TemplateVariables)(implicit user: UserContext): Iterable[Identifier] = {
    val allCurrentVariables = project.combinedTemplateVariables.all
    val allNewVariables = GlobalTemplateVariables.all merge newVariables

    val currentContext: PluginContext = PluginContext.fromProject(project)
    val newContext: PluginContext =
      PluginContext(prefixes = project.config.prefixes,
        resources = project.resources,
        user = user,
        projectId = Some(project.config.id),
        templateVariables = InMemoryTemplateVariablesReader(allNewVariables, currentContext.templateVariables.scopes))

    val updatedTasks = mutable.Buffer[(Identifier, TaskSpec)]()
    for (task <- project.allTasks) {
      try {
        if (hasUpdatedTemplateValues(task.parameters(currentContext), allCurrentVariables, allNewVariables)) {
          updatedTasks.append((task.id, task.withParameters(task.parameters(currentContext), dropExistingValues = true)(newContext)))
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






