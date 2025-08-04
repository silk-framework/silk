package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Prefixes
import org.silkframework.dataset._
import org.silkframework.plugins.dataset.rdf.formatters.{AlignmentLinkFormatter, FormattedLinkSink}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

@Plugin(
  id = "alignment",
  label = "Alignment",
  categories = Array(DatasetCategories.file),
  description =
    """Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html."""
)
case class AlignmentDataset(
  @Param("The alignment file.")
  file: WritableResource) extends Dataset with ResourceBasedDataset {

  override def mimeType: Option[String] = None

  /**
   * Returns a data source for reading entities from the data set.
   */
  override def source(implicit userContext: UserContext): DataSource = throw new UnsupportedOperationException("This dataset only support writing alignments.")

  /**
   * Returns a link sink for writing data to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = new FormattedLinkSink(file, new AlignmentLinkFormatter)

  /**
   * Returns a entity sink for writing data to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = {
    new AlignmentEntitySink()
  }

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()

  /**
   * The alignment dataset cannot write generic entities, but it needs to support the clear method.
   */
  private class AlignmentEntitySink extends EntitySink {

    override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean)
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit = throwNotSupportedException
    override def closeTable()(implicit userContext: UserContext): Unit = throwNotSupportedException
    override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])(implicit userContext: UserContext): Unit = throwNotSupportedException

    private def throwNotSupportedException: Nothing = {
      throw new UnsupportedOperationException("The Alignment dataset only supports writing links. Writing entities is not supported.")
    }
  }
}
