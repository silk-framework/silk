package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset._
import org.silkframework.plugins.dataset.rdf.formatters.{AlignmentLinkFormatter, FormattedLinkSink}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource

@Plugin(
  id = "alignment",
  label = "Alignment file",
  description =
    """ Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html."""
)
case class AlignmentDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource) extends Dataset with WritableResourceDataset with ResourceBasedDataset {

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
  override def entitySink(implicit userContext: UserContext): EntitySink = ???

  override def replaceWritableResource(writableResource: WritableResource): WritableResourceDataset = {
    this.copy(file = writableResource)
  }
}
