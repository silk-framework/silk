package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{QueryFactory, Syntax}
import org.apache.jena.riot.ResultSetMgr
import org.apache.jena.riot.resultset.ResultSetLang
import org.silkframework.dataset.rdf._
import org.silkframework.util.LegacyTraversable

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
    SparqlResults(
      bindings = new ResultsTraversable(query, queryExecutor, params, limit)
    )
  }

  /**
    * Executes a query and passes the result to a processing function.
    */
  trait QueryExecutor {
    def execute(query: String, processResult: InputStream => Unit): Unit
  }

  private class ResultsTraversable(query: String,
                                   queryExecutor: QueryExecutor,
                                   params: SparqlParams,
                                   limit: Int) extends LegacyTraversable[SortedMap[String, RdfNode]] {

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      val parsedQuery = QueryFactory.create(query)
      // Don't set graph if the query is already containing a GRAPH pattern (not easily possible to check with parsed query)
      if(graphPatternRegex.findFirstIn(query).isEmpty && parsedQuery.getGraphURIs.size() == 0) {
        params.graph foreach { graphURI =>
          parsedQuery.addGraphURI(graphURI)
        }
      }
      // FIXME: Also inject FROM NAMED when GRAPH pattern exists and no FROM NAMED was defined in the original query (breaking change).
      if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
        queryExecutor.execute(parsedQuery.serialize(Syntax.syntaxSPARQL_11), is => outputResults(is, f))
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          if (Thread.currentThread().isInterrupted) {
            return
          }
          if (params.pageSize != Int.MaxValue && limit > params.pageSize) {
            parsedQuery.setOffset(offset)
          }
          if (limit != Int.MaxValue || params.pageSize != Int.MaxValue) {
            parsedQuery.setLimit(math.min(params.pageSize, limit - offset))
          }
          queryExecutor.execute(parsedQuery.serialize(Syntax.syntaxSPARQL_11), { inputStream =>
            val resultCount = outputResults(inputStream, f)
            logger.fine("Run: " + offset + ", " + limit + ", " + params.pageSize)
            if (resultCount < params.pageSize) {
              return
            }
          })
        }
      }
    }

    def outputResults[U](inputStream: InputStream, f: SortedMap[String, RdfNode] => U): Int = {
      val resultSet = ResultSetMgr.read(inputStream, ResultSetLang.RS_XML)
      JenaResultsReader.read(resultSet, f)
    }

    override def toString(): String = {
      s"Results of query: $query"
    }
  }
}
