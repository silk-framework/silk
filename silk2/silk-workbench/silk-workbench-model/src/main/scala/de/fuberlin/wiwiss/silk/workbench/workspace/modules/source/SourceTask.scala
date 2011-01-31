package de.fuberlin.wiwiss.silk.workbench.workspace.modules.source

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.datasource.Source

/**
 * A data source.
 */
case class SourceTask(source : Source) extends ModuleTask
{
  val name = source.id
}