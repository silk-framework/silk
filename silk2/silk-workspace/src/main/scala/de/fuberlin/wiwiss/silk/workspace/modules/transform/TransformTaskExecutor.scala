package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.{DataSource, DataSink}
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class TransformTaskExecutor extends TaskExecutor[TransformSpecification] {

  def apply(inputs: Seq[DataSource], transformSpec: TransformSpecification, outputs: Seq[DataSink]) = {
    require(inputs.size == 1, "Transform tasks expect exactly one input dataset.")

    val input = inputs.head
    val activity = new ExecuteTransform(input, transformSpec.selection, transformSpec.rules, outputs)
    activity
  }
}
