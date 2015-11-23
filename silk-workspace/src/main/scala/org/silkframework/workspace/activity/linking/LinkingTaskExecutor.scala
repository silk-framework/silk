package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.{Dataset, DataSink, DataSource}
import org.silkframework.execution.GenerateLinks
import org.silkframework.runtime.activity.Activity
import org.silkframework.util.DPair
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkSpecification] {

  def apply(inputs: Seq[DataSource], linkSpec: LinkSpecification, outputs: Seq[DataSink]) = {
    require(inputs.size == 1 || inputs.size == 2, "Linking tasks expect one or two input datasets.")

    val inputPair = if(inputs.size == 1) DPair.fill(inputs.head) else DPair.fromSeq(inputs)
    new GenerateLinks(inputPair, linkSpec, outputs)
  }
}
