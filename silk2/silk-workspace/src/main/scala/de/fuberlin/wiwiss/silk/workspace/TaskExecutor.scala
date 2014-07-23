package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.task.Task
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask

trait TaskExecutor[T <: ModuleTask] {

  def apply(inputs: Seq[Dataset], task: T, outputs: Seq[Dataset]): Task[_]

}
