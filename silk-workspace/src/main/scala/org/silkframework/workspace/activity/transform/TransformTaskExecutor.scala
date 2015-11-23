package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.{DataSink, DataSource}
import org.silkframework.execution.ExecuteTransform
import org.silkframework.workspace.activity.TaskExecutor

class TransformTaskExecutor extends TaskExecutor[TransformSpecification] {

  def apply(inputs: Seq[DataSource], transformSpec: TransformSpecification, outputs: Seq[DataSink]) = {
    require(inputs.size == 1, "Transform tasks expect exactly one input dataset.")

    val input = inputs.head
    val activity = new ExecuteTransform(input, transformSpec.selection, transformSpec.rules, outputs)
    activity
  }
}
