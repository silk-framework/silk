package org.silkframework.plugins.dataset.rdf

import org.silkframework.runtime.plugin.PluginModule

class RdfPlugins extends PluginModule {

  override def pluginClasses =
    Seq(
      classOf[FileDataset],
      classOf[SparqlDataset],
      classOf[AlignmentDataset],
      classOf[InMemoryDataset]
    )

}
