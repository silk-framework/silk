/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import java.io.{OutputStreamWriter, IOException}
import java.net._
import java.util.logging.{Level, Logger}
import javax.xml.bind.DatatypeConverter

import de.fuberlin.wiwiss.silk.dataset.rdf._
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.SparqlParams

import scala.collection.immutable.SortedMap
import scala.io.Source
import scala.xml.{NodeSeq, Elem, XML}

/**
 * Executes queries on a remote SPARQL endpoint.
 */
class RemoteSparqlEndpoint(params: SparqlParams, httpEndpoint: HttpEndpoint = new DefaultHttpEndpoint) extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[RemoteSparqlEndpoint].getName)

  private var lastQueryTime = 0L

  override def toString = "SparqlEndpoint(" + params.uri + ")"

  override def query(sparql: String, limit: Int) = {
    ResultSet(
      bindings = new ResultTraversable(sparql, limit)
    )
  }

  private class ResultTraversable(sparql: String, limit: Int) extends Traversable[SortedMap[String, RdfNode]] {
    @volatile var blankNodeCount = 0

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      if(sparql.toLowerCase.contains("limit ") || sparql.toLowerCase.contains("offset ")) {
        val xml = executeQuery(sparql)
        val resultsXml = xml \ "results" \ "result"
        for (resultXml <- resultsXml) {
          f(parseResult(resultXml))
        }
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          val xml = executeQuery(sparql + " OFFSET " + offset + " LIMIT " + math.min(params.pageSize, limit - offset))
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
        logger.info("Executing query on " + params.uri + "\n" + query)

      val url = params.uri + "?query=" + URLEncoder.encode(query, "UTF-8") + params.queryParameters
      var result: Elem = null
      var retries = 0
      var retryPause = params.retryPause
      while (result == null) {
        try {
          result = httpEndpoint.select(url, params.login)
        }
        catch {
          case ex: IOException => {
            retries += 1
            if (retries > params.retryCount) {
              throw ex
            }
            logger.info("Query on " + params.uri + " failed:\n" + query + "\nError Message: '" + ex.getMessage + "'.\nRetrying in " + retryPause + " ms. (" + retries + "/" + params.retryCount + ")")

            Thread.sleep(retryPause)
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          }
          case ex: Exception => {
            logger.log(Level.SEVERE, "Could not execute query on " + params.uri + ":\n" + query, ex)
            throw ex
          }
        }
      }

      //Return result
      if (logger.isLoggable(Level.FINER)) logger.finer("Query Result\n" + result)
      result
    }
  }

}

private object RemoteSparqlEndpoint {
  /**
   * Opens a new HTTP connection to the endpoint.
   */
  private def openConnection(url: URL, login: Option[(String, String)]): HttpURLConnection = {
    //Open connection
    val httpConnection = url.openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")
    //Set authentication
    for ((user, password) <- login) {
      httpConnection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }

    httpConnection
  }
}




