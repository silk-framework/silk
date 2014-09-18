package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.execution.GenerateLinksTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkingTask] {

  def apply(inputs: Seq[DataSource], task: LinkingTask, outputs: Seq[DataSink]) = {
    require(inputs.size == 2, "Linking tasks expect exactly two input datasets.")

    new GenerateLinksTask(DPair.fromSeq(inputs), task.linkSpec, outputs)
  }
}
