package de.fuberlin.wiwiss.silk.workbench.workspace.modules

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
   *  Retrieves the tasks in this module.
   */
  def tasks : Traversable[TaskType]

  /**
   * Updates a specific task.
   */
  def update(task : TaskType) : Unit

  /**
   * Removes a task from this project.
   */
  def remove(taskId : String) : Unit
}
