package de.fuberlin.wiwiss.silk.workbench.workspace.modules.output

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.util.Identifier

case class OutputTask(name : Identifier, output : Output) extends ModuleTask
{

}