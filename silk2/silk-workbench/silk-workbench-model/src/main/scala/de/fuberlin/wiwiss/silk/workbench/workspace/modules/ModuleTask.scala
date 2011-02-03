package de.fuberlin.wiwiss.silk.workbench.workspace.modules

import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * A Module Task.
 */
trait ModuleTask
{
  /**
   * The unique name of this task.
   */
  val name : Identifier
}
