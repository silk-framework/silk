package org.silkframework.plugins.dataset

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.{DataSource, DatasetAccess, DatasetSpec, DatasetSpecAccess, EmptyDataset, EmptySource, EntitySink, LinkSink, TypedProperty}
import org.silkframework.entity.Link
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * Executor for [[EmptyDataset]]. Reading yields the empty [[EmptySource]]; writing is a no-op.
  */
class EmptyDatasetExecutor extends LocalDatasetExecutor[EmptyDataset.type] {

  override def access(task: Task[DatasetSpec[EmptyDataset.type]], execution: LocalExecution): DatasetAccess = {
    DatasetSpecAccess(task.data, EmptyDatasetExecutor.EmptyDatasetAccess)
  }
}

object EmptyDatasetExecutor {

  private object EmptyDatasetAccess extends DatasetAccess {

    override def source(implicit userContext: UserContext): DataSource = EmptySource

    /** A dummy entity sink that discards everything written to it. */
    override def entitySink(implicit userContext: UserContext): EntitySink = new EntitySink {
      override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                              (implicit userContext: UserContext): Unit = {}

      override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                            (implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

      override def closeTable()(implicit userContext: UserContext): Unit = {}

      override def close()(implicit userContext: UserContext): Unit = {}

      override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {}
    }

    /** A dummy link sink that discards everything written to it. */
    override def linkSink(implicit userContext: UserContext): LinkSink = new LinkSink {
      override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

      override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                            (implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

      override def close()(implicit userContext: UserContext): Unit = {}

      override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {}
    }
  }
}
