package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.execution.GenerateLinksTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkingTask] {

  def apply(inputs: Seq[DataSource], task: LinkingTask, outputs: Seq[DataSink]) = {
    require(inputs.size == 1 || inputs.size == 2, "Linking tasks expect one or two input datasets.")

    val inputPair = if(inputs.size == 1) DPair.fill(inputs.head) else DPair.fromSeq(inputs)
    new GenerateLinksTask(inputPair, task.linkSpec, outputs)
  }
}
