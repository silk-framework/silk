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

import java.io.{IOException, OutputStreamWriter}
import java.net._
import java.util.logging.Logger
import javax.xml.bind.DatatypeConverter

import org.silkframework.dataset.rdf._

import scala.io.Source
import scala.xml.{Elem, XML}

/**
 * Executes queries on a remote SPARQL endpoint.
 *
 */
case class RemoteSparqlEndpoint(val sparqlParams: SparqlParams) extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[RemoteSparqlEndpoint].getName)

  override def toString = sparqlParams.uri

  override def select(query: String, limit: Int) = {
    PagingSparqlTraversable(query, executeSelect, sparqlParams, limit)
  }

  /**
    * Executes a single select query.
    */
  def executeSelect(query: String): Elem = {
    val queryUrl = sparqlParams.uri + "?query=" + URLEncoder.encode(query, "UTF-8") + sparqlParams.queryParameters
    //Open connection
    val httpConnection = new URL(queryUrl).openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")
    //Set authentication
    for ((user, password) <- sparqlParams.login) {
      httpConnection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }

    try {
      val inputStream = httpConnection.getInputStream
      val result = XML.load(inputStream)
      inputStream.close()
      result
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

  override def update(query: String): Unit = {
    //Open a new HTTP connection
    val connection = new URL(sparqlParams.uri).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    //Set authentication
    for ((user, password) <- sparqlParams.login) {
      connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }
    val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    writer.write("query=")
    writer.write(URLEncoder.encode(query, "UTF8"))
    writer.close()

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 != 2) {
      val errorStream = connection.getErrorStream
      if (errorStream != null) {
        val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
        throw new IOException("SPARQL/Update query on " + sparqlParams.uri + " failed with error code " + connection.getResponseCode + ". Error Message: '" + errorMessage + "'. Failed query:\n" + query)
      }
      else {
        throw new IOException("SPARQL/Update query on " + sparqlParams.uri + " failed. Server response: " + connection.getResponseCode + " " + connection.getResponseMessage + ". Failed query:\n" + query)
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



