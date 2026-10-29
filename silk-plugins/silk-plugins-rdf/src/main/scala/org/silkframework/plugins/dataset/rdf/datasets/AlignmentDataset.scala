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
  description = """Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.""",
  documentationFile = "AlignmentDataset.md"
)
case class AlignmentDataset(
  @Param("The alignment file.")
  file: WritableResource) extends Dataset with ResourceBasedDataset {

  override def mimeType: Option[String] = None

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
