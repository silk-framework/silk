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

package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.{IOException, InputStream, OutputStreamWriter}
import java.net._

import javax.xml.bind.DatatypeConverter
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.adapters.RDFReaderFactoryRIOT
import org.silkframework.dataset.rdf._
import org.silkframework.plugins.dataset.rdf.JenaModelTripleIterator
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.HttpURLConnectionUtils._

import scala.io.Source

/**
 * Executes queries on a remote SPARQL endpoint.
 *
 */
case class RemoteSparqlEndpoint(sparqlParams: SparqlParams) extends SparqlEndpoint {

  private val constructSerialization = RDFLanguages.TURTLE

  private final val CONNECTION_TIMEOUT = 10000
  private final val READ_TIMEOUT = 60000

  override def toString: String = sparqlParams.uri

  override def select(query: String, limit: Int)
                     (implicit userContext: UserContext): SparqlResults = {
    PagingSparqlTraversable(query, executeSelect, sparqlParams, limit)
  }

  /**
    * Executes a single select query.
    */
  def executeSelect(query: String): InputStream = {
    val queryUrl = sparqlParams.uri + "?query=" + URLEncoder.encode(query, "UTF-8") + sparqlParams.queryParameters
    //Open connection
    val httpConnection = new URL(queryUrl).openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")
    setConnectionTimeouts(httpConnection)
    //Set authentication
    for ((user, password) <- sparqlParams.login) {
      httpConnection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }

    try {
      checkResponseStatus(httpConnection, "SELECT query")
      httpConnection.getInputStream
    } catch {
      case ex: IOException =>
        val errorStream = httpConnection.getErrorStream
        if (errorStream != null) {
          val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
          throw new IOException(errorMessage, ex)
        } else {
          throw ex
        }
    }
  }

  private def setConnectionTimeouts(httpConnection: HttpURLConnection) = {
    httpConnection.setConnectTimeout(CONNECTION_TIMEOUT)
    httpConnection.setReadTimeout(READ_TIMEOUT)
  }

  override def construct(query: String)
                        (implicit userContext: UserContext): TripleIterator = {
    val queryUrl = sparqlParams.uri + "?query=" + URLEncoder.encode(query, "UTF-8") + sparqlParams.queryParameters
    //Open connection
    val httpConnection = new URL(queryUrl).openConnection.asInstanceOf[HttpURLConnection]
    setConnectionTimeouts(httpConnection)
    httpConnection.setRequestProperty("ACCEPT", constructSerialization.getContentType.getContentType)
    //Set authentication
    for ((user, password) <- sparqlParams.login) {
      httpConnection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }

    try {
      checkResponseStatus(httpConnection, "Construct query")
      val inputStream = httpConnection.getInputStream
      val reader = new RDFReaderFactoryRIOT().getReader(constructSerialization.getName)
      val m = ModelFactory.createDefaultModel()

      //NOTE: will load all statements into memory
      reader.read(m, inputStream, "")

      //NOTE: listStatement() will not produce graph

      JenaModelTripleIterator(m)
    } catch {
      case ex: IOException =>
        val errorStream = httpConnection.getErrorStream
        if (errorStream != null) {
          val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
          throw new IOException(errorMessage, ex)
        } else {
          throw ex
        }
    } finally {
      httpConnection.disconnect()
    }
  }

  private def checkResponseStatus(httpConnection: HttpURLConnection, requestType: String): Unit = {
    val status = httpConnection.getResponseCode
    if (status / 100 != 2) {
      val errorMessage = httpConnection.errorMessage(" Error details: ").getOrElse("")
      throw new ValidationException(s"$requestType failed on endpoint ${sparqlParams.uri} with code: $status.$errorMessage")
    }
  }

  override def update(query: String)
                     (implicit userContext: UserContext): Unit = {
    //Open a new HTTP connection
    val connection = new URL(sparqlParams.uri).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/sparql-update")
    //Set authentication
    for ((user, password) <- sparqlParams.login) {
      connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }
    val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    writer.write(query)
    writer.close()

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 != 2) {
      connection.errorMessage() match {
        case Some(errorMessage) =>
          throw new IOException("SPARQL/Update query on " + sparqlParams.uri + " failed with error code " +
              connection.getResponseCode + ". Error Message: '" + errorMessage + "'. Failed query:\n" + query)
        case None =>
          throw new IOException("SPARQL/Update query on " + sparqlParams.uri + " failed. Server response: " +
              connection.getResponseCode + ". Failed query:\n" + query)
      }
    }
  }

  /**
    *
    * @param sparqlParams the new configuration of the SPARQL endpoint.
    * @return A SPARQL endpoint configured with the new parameters.
    */
  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
    RemoteSparqlEndpoint(sparqlParams)
  }
}



