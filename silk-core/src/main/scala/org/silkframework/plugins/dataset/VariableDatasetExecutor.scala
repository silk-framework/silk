package org.silkframework.plugins.dataset

import org.silkframework.config.Task
import org.silkframework.dataset.{DataSource, DatasetAccess, DatasetSpec, DatasetSpecAccess, EmptySource, EntitySink, LinkSink, VariableDataset}
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[VariableDataset]]. A variable dataset is a placeholder that must be replaced before
  * execution; reading yields an empty source and writing is not supported.
  */
class VariableDatasetExecutor extends LocalDatasetExecutor[VariableDataset] {

  override def access(task: Task[DatasetSpec[VariableDataset]], execution: LocalExecution): DatasetAccess = {
    DatasetSpecAccess(task.data, VariableDatasetExecutor.VariableDatasetAccess)
  }
}

object VariableDatasetExecutor {

  private object VariableDatasetAccess extends DatasetAccess {
    override def source(implicit userContext: UserContext): DataSource = EmptySource
    override def entitySink(implicit userContext: UserContext): EntitySink = error()
    override def linkSink(implicit userContext: UserContext): LinkSink = error()

    private def error(): Nothing =
      throw new RuntimeException("A Variable Dataset cannot be accessed! Only use it in workflows that replace all variable datasets before execution.")
  }
}
