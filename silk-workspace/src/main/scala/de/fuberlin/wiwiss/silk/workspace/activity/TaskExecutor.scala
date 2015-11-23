package de.fuberlin.wiwiss.silk.workspace.activity

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.runtime.activity.Activity

/**
  * Executes a task.
  */
abstract class TaskExecutor[DataType] {

  def apply(inputs: Seq[DataSource], taskData: DataType, outputs: Seq[DataSink]): Activity[_]
}
