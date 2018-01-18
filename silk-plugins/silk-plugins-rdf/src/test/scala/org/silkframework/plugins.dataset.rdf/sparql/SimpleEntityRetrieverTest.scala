package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint

class SimpleEntityRetrieverTest extends EntityRetrieverBaseTest {
  behavior of "Simple Entity Retriever"

  override def entityRetriever(endpoint: SparqlEndpoint, graphUri: Option[String], useOrderBy: Boolean): SimpleEntityRetriever = {
    new SimpleEntityRetriever(endpoint, SimpleEntityRetriever.DEFAULT_PAGE_SIZE, graphUri, useOrderBy, useSubSelect = true)
  }
}
