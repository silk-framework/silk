package de.fuberlin.wiwiss.silk.workbench.workspace

trait Project
{
  /**
   * Retrieves the project configuration.
   */
  def config : ProjectConfig

  /**
   * Updates the project configuration.
   */
  def config_=(config : ProjectConfig) : Unit

  /**
   * Retrieves the modules in this project.
   */
  def modules : Traversable[Module]

  /**
   * Updates a specific module.
   */
  def update(module : Module) : Unit

  /**
   * Removes a module from this project.
   */
  def remove(module : Module) : Unit
}
