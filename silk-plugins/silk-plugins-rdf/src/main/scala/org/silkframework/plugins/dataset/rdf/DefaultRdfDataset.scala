package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.Dataset
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}

/**
  * Resolves the RDF dataset configured under `dataset.defaultRdf`.
  *
  * Used by SPARQL query tasks that want to submit their query directly to a configured dataset
  * rather than to an RDF dataset wired up via an input port.
  */
object DefaultRdfDataset {

  private val configKey = "dataset.defaultRdf"

  def resolve()(implicit pluginContext: PluginContext): RdfDataset = {
    PluginRegistry.createFromConfigOption[Dataset](configKey) match {
      case Some(rdf: RdfDataset) =>
        rdf
      case Some(other) =>
        throw new IllegalStateException(
          s"Plugin configured at '$configKey' is not an RdfDataset: ${other.getClass.getSimpleName}")
      case None =>
        throw new IllegalStateException(s"No default RDF dataset configured at '$configKey'.")
    }
  }
}
