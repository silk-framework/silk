package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.IOException
import java.util.logging.{Level, Logger}

import org.silkframework.dataset.rdf._

import scala.collection.immutable.SortedMap
import scala.xml.{Elem, NodeSeq}

/**
  * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
  */
object PagingSparqlTraversable {

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
    *
    * @param query The original query. If the query already contains an limit or offset automatic paging is disabled
    * @param queryExecutor A function that executes a SPARQL query and returns the XML result.
    * @param params The SPARQL parameters
    * @param limit The maximum number of SPARQL results returned in total (not per single query)
    */
  def apply(query: String, queryExecutor: String => Elem, params: SparqlParams, limit: Int) = {
    SparqlResults(
      bindings = new ResultsTraversable(query, queryExecutor, params, limit)
    )
  }

  private class ResultsTraversable(query: String, queryExecutor: String => Elem, params: SparqlParams, limit: Int) extends Traversable[SortedMap[String, RdfNode]] {

    private var blankNodeCount = 0

    private var lastQueryTime = 0L

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      if (query.toLowerCase.contains("limit ") || query.toLowerCase.contains("offset ")) {
        val xml = executeQuery(query)
        val resultsXml = xml \ "results" \ "result"
        for (resultXml <- resultsXml) {
          f(parseResult(resultXml))
        }
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          val xml = executeQuery(query + " OFFSET " + offset + " LIMIT " + math.min(params.pageSize, limit - offset))
          val resultsXml = xml \ "results" \ "result"
          for (resultXml <- resultsXml) {
            f(parseResult(resultXml))
          }
          if (resultsXml.size < params.pageSize) return
        }
      }
    }

    private def parseResult(resultXml: NodeSeq): SortedMap[String, RdfNode] = {
      val bindings = resultXml \ "binding"

      val uris = for (binding <- bindings; node <- binding \ "uri") yield ((binding \ "@name").text, Resource(node.text))

      val literals = for (binding <- bindings; node <- binding \ "literal") yield ((binding \ "@name").text, Literal(node.text))

      val bnodes = for (binding <- bindings; node <- binding \ "bnode") yield {
        blankNodeCount += 1
        ((binding \ "@name").text, BlankNode("bnode" + blankNodeCount))
      }

      SortedMap(uris ++ literals ++ bnodes: _*)
    }

    /**
      * Executes a SPARQL SELECT query.
      *
      * @param query The SPARQL query to be executed
      * @return Query result in SPARQL Query Results XML Format
      */
    private def executeQuery(query: String): Elem = {
      //Wait until pause time is elapsed since last query
      synchronized {
        while (System.currentTimeMillis < lastQueryTime + params.pauseTime) Thread.sleep(params.pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }

      //Execute query
      if (logger.isLoggable(Level.FINE))
        logger.fine("Executing query on \n" + query)

      var result: Elem = null
      var retries = 0
      var retryPause = params.retryPause
      while (result == null) {
        try {
          result = queryExecutor(query)
        }
        catch {
          case ex: IOException => {
            retries += 1
            if (retries > params.retryCount) {
              throw ex
            }
            logger.info("Query failed:\n" + query + "\nError Message: '" + ex.getMessage + "'.\nRetrying in " + retryPause + " ms. (" + retries + "/" + params.retryCount + ")")

            Thread.sleep(retryPause)
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          }
          case ex: Exception => {
            logger.log(Level.SEVERE, "Could not execute query:\n" + query, ex)
            throw ex
          }
        }
      }

      //Return result
      result
    }
  }

}
