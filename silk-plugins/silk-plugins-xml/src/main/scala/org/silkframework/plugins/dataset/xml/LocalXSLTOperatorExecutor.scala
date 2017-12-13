package org.silkframework.plugins.dataset.xml

import org.silkframework.config.Task
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.local.{EntityTable, LocalExecution, LocalExecutor}
import org.silkframework.runtime.activity.ActivityContext

/**
  * Execute XSLT script on the XML file of the input dataset and returns a [[org.silkframework.runtime.resource.Resource]]
  * which is written to the resource based dataset. The writing to the target dataset happens in the dataset executor.
  */
case class LocalXSLTOperatorExecutor() extends LocalExecutor[XSLTOperator] {
  override def execute(task: Task[XSLTOperator],
                       inputs: Seq[EntityTable],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    None // TODO: Execute XSLT script
  }
}
