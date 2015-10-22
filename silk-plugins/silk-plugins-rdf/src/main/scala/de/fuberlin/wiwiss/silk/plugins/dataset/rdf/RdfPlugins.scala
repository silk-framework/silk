package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters.{AlignmentFormatter, NTriplesFormatter}
import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

class RdfPlugins extends PluginModule {

  override def pluginClasses =
    Seq(
      classOf[FileDataset],
      classOf[SparqlDataset],
      classOf[AlignmentDataset]
    )

}
