package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.{DataSource, SinkTrait}
import org.silkframework.execution.ExecuteTransform
import org.silkframework.workspace.activity.TaskExecutor

class TransformTaskExecutor extends TaskExecutor[TransformSpecification] {

  override def apply(inputs: Seq[DataSource], transformSpec: TransformSpecification, outputs: Seq[SinkTrait], errorOutputs: Seq[SinkTrait]) = {
    require(inputs.size == 1, "Transform tasks expect exactly one input dataset.")

    val input = inputs.head
    val activity = new ExecuteTransform(input, transformSpec.selection, transformSpec.rules, outputs map (_.entitySink), errorOutputs map (_.entitySink))
    activity
  }
}
