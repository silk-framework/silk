package org.silkframework.workspace.activity.linking

import org.silkframework.dataset._
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.execution.GenerateLinks
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.util.DPair
import org.silkframework.workspace.activity.TaskExecutor

class LinkingTaskExecutor extends TaskExecutor[LinkSpec] {

  override def apply(inputs: Seq[DataSource], linkSpec: LinkSpec, outputs: Seq[DatasetWriteAccess],
                     errorOutputs: Seq[DatasetWriteAccess])
                    (implicit userContext: UserContext): Activity[_ <: ExecutionReport] = {
    require(inputs.size == 1 || inputs.size == 2, "Linking tasks expect one or two input datasets.")

    val inputPair = if(inputs.size == 1) DPair.fill(inputs.head) else DPair.fromSeq(inputs)
    new GenerateLinks("LinkingTask", inputPair, linkSpec, outputs map (_.linkSink)) // TODO: Output errors?
  }
}
