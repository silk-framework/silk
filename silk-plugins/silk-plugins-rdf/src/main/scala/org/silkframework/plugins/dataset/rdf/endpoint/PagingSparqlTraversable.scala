package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{Query, QueryFactory, Syntax}
import org.apache.jena.riot.ResultSetMgr
import org.apache.jena.riot.resultset.ResultSetLang
import org.silkframework.dataset.rdf._
import org.silkframework.util.CloseableIterator

import java.io.InputStream
import java.util.logging.Logger
import scala.collection.immutable.SortedMap
import scala.util.matching.Regex

/**
  * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
  */
object PagingSparqlTraversable {

  val graphPatternRegex: Regex = """[Gg][Rr][Aa][Pp][Hh]\s+[<?]\S+\s*\{""".r

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
    *
    * @param query The original query. If the query already contains an limit or offset automatic paging is disabled
    * @param queryExecutor A function that executes a SPARQL query and returns a Traversable over the SPARQL results.
    * @param params The SPARQL parameters
    * @param limit The maximum number of SPARQL results returned in total (not per single query)
    */
  def apply(query: String, queryExecutor: QueryExecutor, params: SparqlParams, limit: Int): SparqlResults = {
    val parsedQuery = QueryFactory.create(query)
    // Don't set graph if the query is already containing a GRAPH pattern (not easily possible to check with parsed query)
    if (graphPatternRegex.findFirstIn(query).isEmpty && parsedQuery.getGraphURIs.size() == 0) {
      params.graph foreach { graphURI =>
        parsedQuery.addGraphURI(graphURI)
      }
    }
    // FIXME: Also inject FROM NAMED when GRAPH pattern exists and no FROM NAMED was defined in the original query (breaking change).
    val bindings =
      if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
        queryExecutor.executeAndParse(parsedQuery)
      } else {
        new ResultIterator(parsedQuery, queryExecutor, params.pageSize, limit)
      }

    SparqlResults(bindings)
  }

  /**
    * Executes a SPARQL query.
    */
  trait QueryExecutor {

    def execute(query: String): InputStream

    def executeAndParse(query: Query): CloseableIterator[SortedMap[String, RdfNode]] = {
      val inputStream = execute(query.serialize(Syntax.syntaxSPARQL_11))
      val resultSet = ResultSetMgr.read(inputStream, ResultSetLang.RS_XML)
      JenaResultsReader.read(resultSet).thenClose(inputStream)
    }
  }

  private class ResultIterator(parsedQuery: Query, queryExecutor: QueryExecutor, pageSize: Int, limit: Int) extends CloseableIterator[SortedMap[String, RdfNode]] {

    private var offset = 0

    private var count = 0

    private var currentIterator: CloseableIterator[SortedMap[String, RdfNode]] = executeQuery()

    override def hasNext: Boolean = {
      if (currentIterator.hasNext) {
        currentIterator.hasNext
      } else {
        nextPage()
      }
    }

    override def next(): SortedMap[String, RdfNode] = {
      if(currentIterator.hasNext) {
        count += 1
        currentIterator.next()
      } else {
        if(!nextPage()) {
          throw new NoSuchElementException("no more results")
        }
        currentIterator.next()
      }
    }

    /**
      * Retrieves the next page.
      *
      * @return True, if there are more results. False, otherwise.
      */
    private def nextPage(): Boolean = {
      offset += pageSize
      if (count < pageSize) {
        count = 0
        false
      } else {
        count = 0
        currentIterator.close()
        currentIterator = executeQuery()
        currentIterator.hasNext
      }
    }

    /**
      * Executes the query with the current offset.
      */
    private def executeQuery(): CloseableIterator[SortedMap[String, RdfNode]] = {
      if (pageSize != Int.MaxValue && limit > pageSize) {
        parsedQuery.setOffset(offset)
      }
      if (limit != Int.MaxValue || pageSize != Int.MaxValue) {
        parsedQuery.setLimit(math.min(pageSize, limit - offset))
      }
      queryExecutor.executeAndParse(parsedQuery)
    }

    override def close(): Unit = {
      currentIterator.close()
    }
  }
}
