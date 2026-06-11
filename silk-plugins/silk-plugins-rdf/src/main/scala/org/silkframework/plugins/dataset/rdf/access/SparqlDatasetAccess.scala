package org.silkframework.plugins.dataset.rdf.access

import org.silkframework.dataset.rdf.{RdfDatasetAccess, SparqlEndpoint, SparqlParams}
import org.silkframework.dataset.{DataSource, EntitySink, LinkSink}
import org.silkframework.runtime.activity.UserContext

/**
  * Execution-scoped RDF access backed by a SPARQL endpoint.
  *
  * Builds [[SparqlSource]] / [[SparqlSink]] over the given endpoint. Used by the dataset executors of
  * the SPARQL-based datasets (SPARQL endpoint, RDF in-memory, ...) instead of the dataset plugin
  * providing the access itself.
  */
case class SparqlDatasetAccess(params: SparqlParams,
                               sparqlEndpoint: SparqlEndpoint,
                               dropGraphOnClear: Boolean) extends RdfDatasetAccess {

  override def source(implicit userContext: UserContext): DataSource =
    new SparqlSource(params, sparqlEndpoint)

  override def entitySink(implicit userContext: UserContext): EntitySink =
    new SparqlSink(params, sparqlEndpoint, dropGraphOnClear = dropGraphOnClear)

  override def linkSink(implicit userContext: UserContext): LinkSink =
    new SparqlSink(params, sparqlEndpoint, dropGraphOnClear = dropGraphOnClear)
}
