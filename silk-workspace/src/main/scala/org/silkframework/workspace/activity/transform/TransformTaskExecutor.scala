package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.{CombinedEntitySink, DataSource, DatasetWriteAccess}
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.activity.TaskExecutor

class TransformTaskExecutor extends TaskExecutor[TransformSpec] {

  override def apply(inputs: Seq[DataSource], transformSpec: TransformSpec, outputs: Seq[DatasetWriteAccess], errorOutputs: Seq[DatasetWriteAccess]) = {
    require(inputs.size == 1, "Transform tasks expect exactly one input dataset.")

    val input = inputs.head
    val activity = new ExecuteTransform(input, transformSpec, new CombinedEntitySink(outputs.map(_.entitySink)))
    activity
  }
}
