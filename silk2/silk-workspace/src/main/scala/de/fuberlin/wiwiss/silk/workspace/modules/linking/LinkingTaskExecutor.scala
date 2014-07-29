package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.execution.GenerateLinksTask
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkingTask] {

  def apply(inputs: Seq[Dataset], task: LinkingTask, outputs: Seq[Dataset]) = {
    require(inputs.size == 2, "Linking tasks expect exactly two input datasets.")

    // Set the id of the inputs to the ones that are expected by the task
    val taskInputs =
      Seq(inputs(0).copy(id = task.linkSpec.datasets.source.datasetId),
          inputs(1).copy(id = task.linkSpec.datasets.target.datasetId))

    new GenerateLinksTask(taskInputs, task.linkSpec, outputs)
  }
}
