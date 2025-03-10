package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{Query, QueryFactory, Syntax}
import org.apache.jena.riot.ResultSetMgr
import org.apache.jena.riot.resultset.ResultSetLang
import org.silkframework.dataset.rdf._
import org.silkframework.runtime.iterator.{CloseableIterator, RepeatedIterator}

import java.io.InputStream
import java.util.logging.Logger
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters.ListHasAsScala
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
    if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
      queryExecutor.executeAndParse(parsedQuery)
    } else {
      val resultIterator = new ResultIterator(parsedQuery, queryExecutor, params.pageSize, limit)
      SparqlResults(resultIterator.variables, resultIterator)
    }
  }

  /**
    * Executes a SPARQL query.
    */
  trait QueryExecutor {

    def execute(query: String): InputStream

    def executeAndParse(query: Query): SparqlResults = {
      val inputStream = execute(query.serialize(Syntax.syntaxSPARQL_11))
      val resultSet = ResultSetMgr.read(inputStream, ResultSetLang.RS_JSON)
      val results = JenaResultsReader.read(resultSet).thenClose(inputStream)
      SparqlResults(resultSet.getResultVars.asScala.toSeq, results)
    }
  }

  private class ResultIterator(parsedQuery: Query, queryExecutor: QueryExecutor, pageSize: Int, limit: Int) extends CloseableIterator[SortedMap[String, RdfNode]] {

    private var count = 0

    private var offset = 0

    private var currentVariables: Seq[String] = Seq.empty

    private val entityIterator = new RepeatedIterator(nextPage)

    override def hasNext: Boolean = {
      entityIterator.hasNext
    }

    override def next(): SortedMap[String, RdfNode] = {
      val entity = entityIterator.next()
      count += 1
      entity
    }

    override def close(): Unit = {
      entityIterator.close()
    }

    def variables: Seq[String] = {
      currentVariables
    }

    /**
      * Retrieves the next page.
      */
    private def nextPage(): Option[CloseableIterator[SortedMap[String, RdfNode]]] = {
      if (offset > 0 && count < pageSize) {
        count = 0
        None
      } else {
        val results = executeQuery()
        count = 0
        offset += pageSize
        currentVariables = results.variables
        Some(results.bindings)
      }
    }

    /**
      * Executes the query with the current offset.
      */
    private def executeQuery(): SparqlResults = {
      if (pageSize != Int.MaxValue && limit > pageSize) {
        parsedQuery.setOffset(offset)
      }
      if (limit != Int.MaxValue || pageSize != Int.MaxValue) {
        parsedQuery.setLimit(math.min(pageSize, limit - offset))
      }
      queryExecutor.executeAndParse(parsedQuery)
    }
  }
}
