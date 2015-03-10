package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity

trait TaskExecutor[DataType] {

  def apply(inputs: Seq[DataSource], taskData: DataType, outputs: Seq[DataSink]): Activity[_]

}
