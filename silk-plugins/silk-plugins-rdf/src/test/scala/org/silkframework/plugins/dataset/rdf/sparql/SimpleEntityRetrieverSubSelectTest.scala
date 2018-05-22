package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint

class SimpleEntityRetrieverSubSelectTest extends EntityRetrieverBaseTest {
  behavior of "Simple Entity Retriever with subselect enabled"

  override def entityRetriever(endpoint: SparqlEndpoint, graphUri: Option[String], useOrderBy: Boolean): SimpleEntityRetriever = {
    new SimpleEntityRetriever(endpoint, SimpleEntityRetriever.DEFAULT_PAGE_SIZE, graphUri, useOrderBy, useSubSelect = true)
  }
}
