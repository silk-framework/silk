package org.silkframework.workspace

import org.silkframework.config.{MetaData, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.exceptions.{CircularDependencyException, TaskValidationException}

import scala.collection.immutable.ListMap

/**
  * Validates a task in a project context.
  */
trait TaskValidator[TaskType <: TaskSpec] {

  /**
    * Validates a task in a project context.
    * This method should be called before adding a task to a project.
    * Returns silently if no issue has been found.
    *
    * @throws TaskValidationException If the task validation failed
    */
  def validate(project: Project, task: Task[TaskType])
              (implicit userContext: UserContext): Unit

}

/**
  * Default task validator that checks for circular dependencies.
  * More specific task validators should derive from this class and call super.validate in their validate method.
  */
class DefaultTaskValidator[TaskType <: TaskSpec] extends TaskValidator[TaskType] {

  def validate(project: Project, task: Task[TaskType])
              (implicit userContext: UserContext): Unit = {
    checkForRecursion(project, task.id, task.data, task.metaData)
  }

  /**
    * Checks if task references to a new task would create a circular chain of dependencies.
    * Returns silently if no issues have been found.
    *
    * @param id Identifier of the new task
    * @param task Task itself whose references are to be checked
    * @param metaData Task metadata
    * @param referencedTaskChain The chain of referenced tasks. Map from the task identifiers to the task data and metadata.
    * @throws CircularDependencyException If a circular dependency has been found.
    */
  def checkForRecursion(project: Project, id: Identifier, task: TaskSpec, metaData: MetaData, referencedTaskChain: ListMap[Identifier, (TaskSpec, MetaData)] = ListMap.empty)
                       (implicit userContext: UserContext): Unit = {
    if(referencedTaskChain.contains(id)) {
      val taskChain = referencedTaskChain.keys.toSeq :+ id
      val taskLabels = taskChain.map(id => referencedTaskChain(id)._2.formattedLabel(id))
      throw CircularDependencyException(taskLabels)
    } else {
      val updatedTaskChain = referencedTaskChain + (id -> (task, metaData))
      for {
        refTaskId <- task.referencedTasks
        (refTask, refMetaData) <- updatedTaskChain.get(refTaskId).orElse(project.anyTaskOption(refTaskId).map(t => (t.data, t.metaData)))
      } {
        checkForRecursion(project, refTaskId, refTask, refMetaData, updatedTaskChain)
      }
    }
  }

}
