package org.silkframework.plugins.dataset.rdf

import org.silkframework.plugins.dataset.rdf.formatters.{AlignmentFormatter, NTriplesFormatter}
import org.silkframework.runtime.plugin.PluginModule

class RdfPlugins extends PluginModule {

  override def pluginClasses =
    Seq(
      classOf[InternalDataset],
      classOf[FileDataset],
      classOf[SparqlDataset],
      classOf[AlignmentDataset]
    )

}
