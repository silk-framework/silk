package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams, SparqlResults}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.UserContext

import scala.collection.mutable.ArrayBuffer

class ParallelEntityRetrieverTest extends EntityRetrieverBaseTest {
  behavior of "Parallel entity retriever"

  override def entityRetriever(endpoint: SparqlEndpoint,
                               graphUri: Option[String] = None,
                               useOrderBy: Boolean): ParallelEntityRetriever = {
    new ParallelEntityRetriever(endpoint, SimpleEntityRetriever.DEFAULT_PAGE_SIZE, graphUri, useOrderBy)
  }
}

case class TestMockSparqlEndpoint(sparqlParams: SparqlParams) extends SparqlEndpoint {

  private val queryQueue = new ArrayBuffer[(String, Int)]()

  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = TestMockSparqlEndpoint(sparqlParams)

  override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
    queryQueue.append((query, limit))
    SparqlResults(Seq.empty)
  }

  def queries: Seq[(String, Int)] = queryQueue

  def clearQueue(): Unit = {
    queryQueue.clear()
  }
}
