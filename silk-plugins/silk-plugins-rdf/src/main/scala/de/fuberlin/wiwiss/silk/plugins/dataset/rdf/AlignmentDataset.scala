package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import de.fuberlin.wiwiss.silk.dataset.{DataSource, DataSink, DatasetPlugin}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters.{AlignmentFormatter, FormattedDataSink}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.{WritableResource, Resource}

@Plugin(
  id = "alignment",
  label = "Alignment",
  description =
    """ Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
      | Parameters:
      |  file: File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
    """
)
case class AlignmentDataset(file: WritableResource) extends DatasetPlugin {
  /**
   * Returns a data source for reading entities from the data set.
   */
  override def source: DataSource = throw new UnsupportedOperationException("This dataset only support writing alignments.")

  /**
   * Returns a data sink for writing data to the data set.
   */
  override def sink: DataSink = new FormattedDataSink(file, new AlignmentFormatter)
}
