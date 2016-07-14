package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset._
import org.silkframework.plugins.dataset.rdf.formatters.{AlignmentLinkFormatter, FormattedLinkSink}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource

@Plugin(
  id = "alignment",
  label = "Alignment",
  description =
    """ Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html."""
)
case class AlignmentDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource) extends DatasetPlugin with WritableResourceDatasetPlugin {

  /**
   * Returns a data source for reading entities from the data set.
   */
  override def source: DataSource = throw new UnsupportedOperationException("This dataset only support writing alignments.")

  /**
   * Returns a link sink for writing data to the data set.
   */
  override def linkSink: LinkSink = new FormattedLinkSink(file, new AlignmentLinkFormatter)

  /**
   * Returns a entity sink for writing data to the data set.
   */
  override def entitySink: EntitySink = ???

  override def clear(): Unit = { }

  override def replaceWritableResource(writableResource: WritableResource): WritableResourceDatasetPlugin = {
    this.copy(file = writableResource)
  }
}
