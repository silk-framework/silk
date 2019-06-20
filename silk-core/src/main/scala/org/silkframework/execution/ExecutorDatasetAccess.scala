package org.silkframework.execution

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetAccess, DatasetSpec, EntitySink, LinkSink}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

/**
  * This trait provides sources and sinks based on the LocalExecutor of the base dataset.
  * Can be removed as soon as Datasets no longer derive from DatasetAccess.
  */
trait ExecutorDatasetAccess[DatasetType <: Dataset] extends DatasetAccess {

  protected def datasetName: String

  override def source(implicit userContext: UserContext): DataSource = {
    ExecutorRegistry.access(dummyTask, LocalExecution()).source
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = {
    ExecutorRegistry.access(dummyTask, LocalExecution()).linkSink
  }

  override def entitySink(implicit userContext: UserContext): EntitySink = {
    ExecutorRegistry.access(dummyTask, LocalExecution()).entitySink
  }

  val dummyTask: Task[DatasetSpec[DatasetType]] = {
    PlainTask(Identifier.fromAllowed(datasetName), DatasetSpec(this.asInstanceOf[DatasetType]))
  }

}
