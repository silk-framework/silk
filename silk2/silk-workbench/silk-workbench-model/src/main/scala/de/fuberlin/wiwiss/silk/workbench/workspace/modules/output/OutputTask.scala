package de.fuberlin.wiwiss.silk.workbench.workspace.modules.output

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.output.Output

case class OutputTask(output : Output) extends ModuleTask
{
  val name = output.id
}