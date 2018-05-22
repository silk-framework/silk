package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint

class ParallelEntityRetrieverTest extends EntityRetrieverBaseTest {
  behavior of "Parallel entity retriever"

  override def entityRetriever(endpoint: SparqlEndpoint,
                               graphUri: Option[String] = None,
                               useOrderBy: Boolean): ParallelEntityRetriever = {
    new ParallelEntityRetriever(endpoint, SimpleEntityRetriever.DEFAULT_PAGE_SIZE, graphUri, useOrderBy)
  }
}
