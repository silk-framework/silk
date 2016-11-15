package org.silkframework.workspace

import org.silkframework.dataset.rdf.SparqlEndpoint

/**
  * A workspace provider that holds information in RDF.
  */
trait RdfWorkspaceProvider extends WorkspaceProvider {

  // The RDF store
  def endpoint: SparqlEndpoint

}
