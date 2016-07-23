package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpec
import org.silkframework.dataset._
import org.silkframework.execution.GenerateLinks
import org.silkframework.util.DPair
import org.silkframework.workspace.activity.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkSpec] {

  override def apply(inputs: Seq[DataSource], linkSpec: LinkSpec, outputs: Seq[SinkTrait], errorOutputs: Seq[SinkTrait]) = {
    require(inputs.size == 1 || inputs.size == 2, "Linking tasks expect one or two input datasets.")

    val inputPair = if(inputs.size == 1) DPair.fill(inputs.head) else DPair.fromSeq(inputs)
    new GenerateLinks("LinkingTask", inputPair, linkSpec, outputs map (_.linkSink)) // TODO: Output errors?
  }
}
