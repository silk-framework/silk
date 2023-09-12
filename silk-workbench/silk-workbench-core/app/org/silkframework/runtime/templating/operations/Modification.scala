package org.silkframework.runtime.templating.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterTemplateValue, ParameterValues, PluginContext}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.{InMemoryTemplateVariablesReader, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.util.control.NonFatal

abstract class Modification {

  def project: Project

  protected def updateVariables(currentVariables: TemplateVariables): TemplateVariables

  protected def generateException(task: Task[_ <: TaskSpec], cause: Throwable): CannotModifyVariablesUsedByTaskException

  def execute()(implicit user: UserContext): Unit = {
    val currentVariables = project.templateVariables.all
    val newVariables = updateVariables(currentVariables)
    updateTasks(project, newVariables)
    project.templateVariables.put(newVariables)
  }

  private def updateTasks(project: Project, newVariables: TemplateVariables)(implicit user: UserContext): Unit = {
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






