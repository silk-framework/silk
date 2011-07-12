package de.fuberlin.wiwiss.silk.workbench.workspace.modules.output

import de.fuberlin.wiwiss.silk.util.Identifier
import java.util.logging.Logger

/**
 * Output module which holds all outputs in memory.
 */
class MemoryOutputModule extends OutputModule
{
  private val log = Logger.getLogger(classOf[MemoryOutputModule].getName)

  private var outputsTasks = Map[Identifier, OutputTask]()

  def config = OutputConfig()

  def config_=(c: OutputConfig) { }

  override def tasks = synchronized { outputsTasks.values }

  override def update(task : OutputTask) = synchronized
  {
    outputsTasks += (task.name -> task)
    log.info("Updated output '" + task.name)
  }

  override def remove(taskId : Identifier) = synchronized
  {
    outputsTasks -= taskId
    log.info("Removed output '" + taskId)
  }
}
