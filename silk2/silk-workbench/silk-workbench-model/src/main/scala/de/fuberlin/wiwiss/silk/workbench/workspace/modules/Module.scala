package de.fuberlin.wiwiss.silk.workbench.workspace.modules

import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * A project module.
 */
trait Module[ConfigType <: ModuleConfig, TaskType <: ModuleTask]
{
  /**
   * The configuration of this module
   */
  def config : ConfigType

  /**
   * Updates the configuration of this module.
   */
  def config_=(c : ConfigType) : Unit

  /**
   * Retrieves the tasks in this module.
   */
  def tasks : Traversable[TaskType]

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name : Identifier) : TaskType =
  {
    tasks.find(_.name == name).getOrElse(throw new NoSuchElementException("Task '" + name + "' not found."))
  }

  /**
   *  Updates a specific task.
   */
  def update(task : TaskType) : Unit

  /**
   * Removes a task from this project.
   */
  def remove(taskId : Identifier) : Unit
}
