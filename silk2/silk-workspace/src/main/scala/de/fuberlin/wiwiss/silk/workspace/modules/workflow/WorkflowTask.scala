package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask

class WorkflowTask(val name: Identifier) extends ModuleTask {
}

object WorkflowTask {

  case class Task()

}