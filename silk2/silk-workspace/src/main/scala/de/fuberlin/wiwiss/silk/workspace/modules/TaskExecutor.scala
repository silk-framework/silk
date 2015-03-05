package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity

trait TaskExecutor[T <: ModuleTask] {

  def apply(inputs: Seq[DataSource], task: T, outputs: Seq[DataSink]): Activity[_]

}
