package org.silkframework.plugins.dataset.rdf.datasets

import org.silkframework.config.Task
import org.silkframework.dataset.rdf.{RdfDatasetAccess, SparqlEndpoint}
import org.silkframework.dataset.{DataSource, DatasetSpec, EntitySink, LinkSink, TripleSink}
import org.silkframework.execution.local.{LocalExecution, LocalRdfDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.formatters.{FormattedEntitySink, FormattedLinkSink}
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[RdfFileDataset]]. Reads via the bulk-aware data source and writes N-Triples via the
  * formatted sinks; exposes the file-backed Jena SPARQL endpoint.
  */
class RdfFileDatasetExecutor extends LocalRdfDatasetExecutor[RdfFileDataset] {

  override protected def rdfAccess(task: Task[DatasetSpec[RdfFileDataset]], execution: LocalExecution): RdfDatasetAccess = {
    new RdfFileDatasetExecutor.RdfFileDatasetAccess(task.data.plugin)
  }
}

object RdfFileDatasetExecutor {

  private class RdfFileDatasetAccess(plugin: RdfFileDataset) extends RdfDatasetAccess {

    override def sparqlEndpoint: SparqlEndpoint = plugin.sparqlEndpoint

    override def source(implicit userContext: UserContext): DataSource = plugin.createDataSource

    override def entitySink(implicit userContext: UserContext): EntitySink =
      new FormattedEntitySink(plugin.bulkWritableResource, plugin.formatter)

    override def linkSink(implicit userContext: UserContext): LinkSink =
      new FormattedLinkSink(plugin.bulkWritableResource, plugin.formatter)

    override def tripleSink(implicit userContext: UserContext): TripleSink =
      new FormattedEntitySink(plugin.bulkWritableResource, plugin.formatter)
  }
}
