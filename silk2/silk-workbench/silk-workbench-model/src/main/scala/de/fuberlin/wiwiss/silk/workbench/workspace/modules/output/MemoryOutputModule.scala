package de.fuberlin.wiwiss.silk.workbench.workspace.modules.output

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.Module
import de.fuberlin.wiwiss.silk.util.Identifier
import java.util.logging.Logger

class MemoryOutputModule extends Module[OutputConfig, OutputTask]
{
  private val log = Logger.getLogger(classOf[MemoryOutputModule].getName)

  private var outputsTasks = Map[Identifier, OutputTask]()

  def config = OutputConfig()

  def config_=(c: OutputConfig) { }

  override def tasks = synchronized { outputsTasks }

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
