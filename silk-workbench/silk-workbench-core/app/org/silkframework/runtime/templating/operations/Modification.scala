package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterTemplateValue, ParameterValues, PluginContext}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{InMemoryTemplateVariablesReader, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NonFatal

/**
  * Modifies the current project variables.
  * Updates all tasks that use variables that have been modified.
  */
abstract class Modification {

  private val log: Logger = Logger.getLogger(getClass.getName)

  /**
    * The project whose variables are to be modified.
    */
  def project: Project

  /**
    * Brief description of the done operation, e.g., "Deleted variables".
    */
  def operation: String

  /**
    * Implements the concrete variables modification.
    */
  protected def updateVariables(currentVariables: TemplateVariables): TemplateVariables

  /**
    * Generates an exception if a variable could not be updated, because a task would become invalid.
    */
  protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException

  /**
    * Updates the project variables and all tasks that use updated variables.
    */
  def execute()(implicit user: UserContext): Unit = {
    val currentVariables = project.templateVariables.all
    val newVariables = updateVariables(currentVariables)
    val updatedTaskIds = updateTasks(newVariables)
    project.templateVariables.put(newVariables)
    if(updatedTaskIds.nonEmpty) {
      log.info(s"$operation. The following tasks have been updated: " + updatedTaskIds)
    } else {
      log.info(s"$operation. No tasks have been updated.")
    }
  }

  /**
    * Retrieves the tasks that would become invalid by this modification.
    */
  def invalidTasks()(implicit user: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    val currentVariables = project.templateVariables.all
    val newVariables = updateVariables(currentVariables)
    val currentContext: PluginContext = PluginContext.fromProject(project)
    val updatedTasks = mutable.Buffer[ProjectTask[_ <: TaskSpec]]()
    for (task <- project.allTasks) yield {
      try {
        hasUpdatedTemplateValues(task.parameters(currentContext), currentVariables, newVariables)
      } catch {
        case _: TemplateEvaluationException =>
          // Task update would fail with the modified variables.
          updatedTasks.append(task)
      }
      task
    }
    updatedTasks.toSeq
  }

  private def updateTasks(newVariables: TemplateVariables)(implicit user: UserContext): Iterable[Identifier] = {
    val currentVariables = project.templateVariables.all

    val currentContext: PluginContext = PluginContext.fromProject(project)
    val newContext: PluginContext =
      PluginContext(prefixes = project.config.prefixes,
        resources = project.resources,
        user = user,
        projectId = Some(project.config.id),
        templateVariables = InMemoryTemplateVariablesReader(newVariables, currentContext.templateVariables.scopes))

    val updatedTasks = mutable.Buffer[(Identifier, TaskSpec)]()
    for (task <- project.allTasks) {
      try {
        if (hasUpdatedTemplateValues(task.parameters(currentContext), currentVariables, newVariables)) {
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

  private def hasUpdatedTemplateValues(parameters: ParameterValues, currentVariables: TemplateVariables, newVariables: TemplateVariables): Boolean = {
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






