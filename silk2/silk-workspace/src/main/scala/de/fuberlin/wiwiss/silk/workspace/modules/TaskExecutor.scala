package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.task.Task

trait TaskExecutor[T <: ModuleTask] {

  def apply(inputs: Seq[Dataset], task: T, outputs: Seq[Dataset]): Task[_]

}
