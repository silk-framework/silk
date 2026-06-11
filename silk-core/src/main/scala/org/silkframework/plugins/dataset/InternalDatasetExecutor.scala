package org.silkframework.plugins.dataset

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}

/**
  * Executor for the deprecated [[InternalDatasetTrait]] datasets. An internal dataset delegates to a
  * configured inner dataset; this resolves the inner dataset's execution-scoped access through the
  * executor registry.
  */
class InternalDatasetExecutor extends LocalDatasetExecutor[InternalDatasetTrait] {

  override def access(task: Task[DatasetSpec[InternalDatasetTrait]], execution: LocalExecution): DatasetAccess = {
    val inner = task.data.plugin.internalDatasetPluginImpl
    ExecutorRegistry.access(PlainTask(task.id, DatasetSpec(inner)), execution)
  }
}
