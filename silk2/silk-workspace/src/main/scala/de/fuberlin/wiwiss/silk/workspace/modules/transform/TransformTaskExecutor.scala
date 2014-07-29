package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class TransformTaskExecutor extends TaskExecutor[TransformTask] {

  def apply(inputs: Seq[Dataset], task: TransformTask, outputs: Seq[Dataset]) = {
    require(inputs.size == 1, "Transform tasks expect exactly one input dataset.")

    val input = inputs.head
    val job = new ExecuteTransform(input, task.dataSelection.copy(datasetId = input.id), task.rules, outputs)
    job
  }
}
