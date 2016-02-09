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

import java.io.{OutputStreamWriter, IOException}
import java.net._
import java.util.logging.{Level, Logger}
import javax.xml.bind.DatatypeConverter

import org.silkframework.dataset.rdf._
import org.silkframework.plugins.dataset.rdf.SparqlParams

import scala.collection.immutable.SortedMap
import scala.io.Source
import scala.xml.{NodeSeq, Elem, XML}

/**
 * Executes queries on a remote SPARQL endpoint.
 *
 */
class RemoteSparqlEndpoint(params: SparqlParams) extends SparqlEndpoint {

  private val logger = Logger.getLogger(classOf[RemoteSparqlEndpoint].getName)

  override def toString = params.uri

  override def select(query: String, limit: Int) = {
    PagingSparqlTraversable(query, executeSelect, params, limit)
  }

  /**
    * Executes a single select query.
    */
  def executeSelect(query: String): Elem = {
    val queryUrl = params.uri + "?query=" + URLEncoder.encode(query, "UTF-8") + params.queryParameters
    //Open connection
    val httpConnection = new URL(queryUrl).openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")
    //Set authentication
    for ((user, password) <- params.login) {
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
    val connection = new URL(params.uri).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    //Set authentication
    for ((user, password) <- params.login) {
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
        throw new IOException("SPARQL/Update query on " + params.uri + " failed with error code " + connection.getResponseCode + ". Error Message: '" + errorMessage + "'. Failed query:\n" + query)
      }
      else {
        throw new IOException("SPARQL/Update query on " + params.uri + " failed. Server response: " + connection.getResponseCode + " " + connection.getResponseMessage + ". Failed query:\n" + query)
      }
    }
  }

}



