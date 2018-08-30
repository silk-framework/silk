package org.silkframework.execution.local

import org.silkframework.execution.ExecutionManager

case class LocalExecutionManager() extends ExecutionManager {

  private lazy val execution = LocalExecution(useLocalInternalDatasets = false)

  override def current(): LocalExecution = execution
}
