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

import java.io.IOException
import java.net._
import java.util.logging.{Level, Logger}
import javax.xml.bind.DatatypeConverter

import de.fuberlin.wiwiss.silk.dataset.rdf._

import scala.io.Source
import scala.xml.{Elem, XML}

/**
 * Executes queries on a remote SPARQL endpoint.
 *
 * @param uri The URI of the endpoint
 * @param login The login required by the endpoint for authentication
 * @param pageSize The number of solutions to be retrieved per SPARQL query (default: 1000)
 * @param pauseTime The minimum number of milliseconds between two queries
 * @param retryCount The number of retries if a query fails
 * @param initialRetryPause The pause in milliseconds before a query is retried. For each subsequent retry the pause is doubled.
 */
class RemoteSparqlEndpoint(val uri: URI,
                           login: Option[(String, String)] = None,
                           val pageSize: Int = 1000, val pauseTime: Int = 0,
                           val retryCount: Int = 3, val initialRetryPause: Int = 1000,
                           val queryParameters: String = "") extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[RemoteSparqlEndpoint].getName)

  private var lastQueryTime = 0L

  override def toString = "SparqlEndpoint(" + uri + ")"

  override def query(sparql: String, limit: Int) = {
    ResultSet(
      bindings = new ResultTraversable(sparql, limit)
    )
  }

  private class ResultTraversable(sparql: String, limit: Int) extends Traversable[Map[String, RdfNode]] {
    override def foreach[U](f: Map[String, RdfNode] => U): Unit = {
      var blankNodeCount = 0

      for (offset <- 0 until limit by pageSize) {
        val xml = executeQuery(sparql + " OFFSET " + offset + " LIMIT " + math.min(pageSize, limit - offset))

        val resultsXml = xml \ "results" \ "result"

        for (resultXml <- resultsXml) {
          val bindings = resultXml \ "binding"

          val uris = for (binding <- bindings; node <- binding \ "uri") yield ((binding \ "@name").text, Resource(node.text))

          val literals = for (binding <- bindings; node <- binding \ "literal") yield ((binding \ "@name").text, Literal(node.text))

          val bnodes = for (binding <- bindings; node <- binding \ "bnode") yield {
            blankNodeCount += 1
            ((binding \ "@name").text, BlankNode("bnode" + blankNodeCount))
          }

          f((uris ++ literals ++ bnodes).toMap)
        }

        if (resultsXml.size < pageSize) return
      }
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
        while (System.currentTimeMillis < lastQueryTime + pauseTime) Thread.sleep(pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }

      //Execute query
      //if (logger.isLoggable(Level.FINE))
      logger.info("Executing query on " + uri + "\n" + query)

      val url = new URL(uri + "?query=" + URLEncoder.encode(query, "UTF-8") + queryParameters)

      var result: Elem = null
      var retries = 0
      var retryPause = initialRetryPause
      while (result == null) {
        val httpConnection = RemoteSparqlEndpoint.openConnection(url, login)

        try {
          val inputStream = httpConnection.getInputStream
          result = XML.load(inputStream)
          inputStream.close()
          httpConnection.disconnect()
        }
        catch {
          case ex: IOException => {
            retries += 1
            if (retries > retryCount) {
              throw ex
            }

            if (logger.isLoggable(Level.INFO)) {
              val errorStream = httpConnection.getErrorStream
              if (errorStream != null) {
                val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
                logger.info("Query on " + uri + " failed:\n" + query + "\nError Message: '" + errorMessage + "'.\nRetrying in " + retryPause + " ms. (" + retries + "/" + retryCount + ")")
              }
              else {
                logger.info("Query on " + uri + " failed:\n" + query + "\nRetrying in " + retryPause + " ms. (" + retries + "/" + retryCount + ")")
              }
            }

            Thread.sleep(retryPause)
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          }
          case ex: Exception => {
            logger.log(Level.SEVERE, "Could not execute query on " + uri + ":\n" + query, ex)
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
