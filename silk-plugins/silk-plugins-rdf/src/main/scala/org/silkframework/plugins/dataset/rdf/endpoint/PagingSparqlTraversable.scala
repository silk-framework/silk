package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.{IOException, InputStream}
import java.util.logging.{Level, Logger}

import javax.xml.stream.XMLInputFactory
import org.apache.jena.query.{QueryFactory, Syntax}
import org.apache.jena.riot.ResultSetMgr
import org.apache.jena.riot.resultset.ResultSetLang
import org.silkframework.dataset.rdf._

import scala.collection.immutable.SortedMap
import scala.util.Try
import scala.util.matching.Regex

/**
  * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
  */
object PagingSparqlTraversable {

  val graphPatternRegex: Regex = """[Gg][Rr][Aa][Pp][Hh]\s+<""".r
  private val xmlFactory = XMLInputFactory.newInstance()

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
    *
    * @param query The original query. If the query already contains an limit or offset automatic paging is disabled
    * @param queryExecutor A function that executes a SPARQL query and returns a Traversable over the SPARQL results.
    * @param params The SPARQL parameters
    * @param limit The maximum number of SPARQL results returned in total (not per single query)
    */
  def apply(query: String, queryExecutor: String => InputStream, params: SparqlParams, limit: Int): SparqlResults = {
    SparqlResults(
      bindings = new ResultsTraversable(query, queryExecutor, params, limit)
    )
  }

  private class ResultsTraversable(query: String,
                                   queryExecutor: String => InputStream,
                                   params: SparqlParams,
                                   limit: Int) extends Traversable[SortedMap[String, RdfNode]] {

    private var lastQueryTime = 0L

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      val parsedQuery = QueryFactory.create(query)
      // Don't set graph if the query is already containing a GRAPH pattern (not easily possible to check with parsed query)
      if(graphPatternRegex.findFirstIn(query).isEmpty) {
        params.graph foreach { graphURI =>
          parsedQuery.addGraphURI(graphURI)
        }
      }

      if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
        val inputStream = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
        try {
          outputResults(inputStream, f)
        } finally {
          inputStream.close()
        }
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          parsedQuery.setLimit(math.min(params.pageSize, limit - offset))
          parsedQuery.setOffset(offset)
          val inputStream = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
          try {
            val resultCount = outputResults(inputStream, f)
            logger.fine("Run: " + offset + ", " + limit + ", " + params.pageSize)
            if (resultCount < params.pageSize) return
          } finally {
            inputStream.close()
          }
        }
      }
    }

    /**
      * Executes a SPARQL SELECT query.
      *
      * @param rewrittenQuery The SPARQL query to be executed
      * @return an XML stream reader positioned on the <results> tag.
      */
    private def executeQuery(rewrittenQuery: String): InputStream = {
      //Wait until pause time is elapsed since last query
      synchronized {
        while (System.currentTimeMillis < lastQueryTime + params.pauseTime) Thread.sleep(params.pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }
      //Execute query
      logger.fine("Executing query on \n" + rewrittenQuery)

      var inputStream: InputStream = null
      var retries = 0
      val retryPause = params.retryPause

      def closeInputStream() = {
        if (inputStream != null) {
          Try(inputStream.close())
        }
      }

      while (inputStream == null) {
        try {
          inputStream = queryExecutor(rewrittenQuery)
        } catch {
          case ex: IOException =>
            retries += 1
            if (retries > params.retryCount) {
              throw ex
            }
            logger.info(s"Query failed:\n$rewrittenQuery\nError Message: '${ex.getMessage}'.\nRetrying in $retryPause ms. ($retries/${params.retryCount})")

            Thread.sleep(retryPause)
            closeInputStream()
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          case ex: Exception =>
            logger.log(Level.SEVERE, "Could not execute query:\n" + rewrittenQuery, ex)
            closeInputStream()
            throw ex
        }
      }
      //Return result
      inputStream
    }

    def outputResults[U](inputStream: InputStream, f: SortedMap[String, RdfNode] => U): Int = {
      val resultSet = ResultSetMgr.read(inputStream, ResultSetLang.SPARQLResultSetXML)
      JenaResultsReader.read(resultSet, f)
    }
  }
}
