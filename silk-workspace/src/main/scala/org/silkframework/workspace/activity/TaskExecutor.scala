package org.silkframework.workspace.activity

import org.silkframework.dataset._
import org.silkframework.runtime.activity.Activity

/**
  * Executes a task.
  */
abstract class TaskExecutor[DataType] {

  def apply(inputs: Seq[DataSource], taskData: DataType, outputs: Seq[SinkTrait]): Activity[_]
}
