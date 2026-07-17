package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution}
import org.silkframework.plugins.dataset.rdf.formatters.{AlignmentLinkFormatter, FormattedLinkSink}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

/**
  * Executor for [[AlignmentDataset]]. Writes links in the alignment format; reading is not supported.
  */
class AlignmentDatasetExecutor extends LocalDatasetExecutor[AlignmentDataset] {

  override def access(task: Task[DatasetSpec[AlignmentDataset]], execution: LocalExecution): DatasetAccess = {
    DatasetSpecAccess(task.data, new AlignmentDatasetExecutor.AlignmentDatasetAccess(task.data.plugin.file))
  }
}

object AlignmentDatasetExecutor {

  private class AlignmentDatasetAccess(file: WritableResource) extends DatasetAccess {

    override def source(implicit userContext: UserContext): DataSource =
      throw new UnsupportedOperationException("This dataset only support writing alignments.")

    override def linkSink(implicit userContext: UserContext): LinkSink =
      new FormattedLinkSink(file, new AlignmentLinkFormatter)

    override def entitySink(implicit userContext: UserContext): EntitySink = new AlignmentEntitySink()

    /** The alignment dataset cannot write generic entities, but it needs to support the clear method. */
    private class AlignmentEntitySink extends EntitySink {
      override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
        file.delete()
      }

      override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean)
                            (implicit userContext: UserContext, prefixes: Prefixes): Unit = throwNotSupportedException
      override def closeTable()(implicit userContext: UserContext): Unit = throwNotSupportedException
      override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])(implicit userContext: UserContext): Unit = throwNotSupportedException

      private def throwNotSupportedException: Nothing = {
        throw new UnsupportedOperationException("The Alignment dataset only supports writing links. Writing entities is not supported.")
      }
    }
  }
}
