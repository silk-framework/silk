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

package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.output.{LinkWriter}
import java.io.{OutputStreamWriter, Writer}
import java.util.logging.Logger
import java.net.{URLEncoder, URL, HttpURLConnection}
import io.Source
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * A link writer which writes to a SPARQL/Update endpoint.
 *
 * @param uri The uri of the SPARQL/Update endpoint e.g. http://localhost:8090/virtuoso/sparql.
 * @param parameter The HTTP parameter used to submit queries. Defaults to "query" which works for most endpoints.
 *                  Some endpoints require different parameters e.g. Sesame expects "update" and Joseki expects "request".
 * @param graphUri The graph all statements are written into. Optional.
 */
@Plugin(id = "sparul", label = "SPARQL/Update", description = "Writes the links to a store using SPARQL/Update.")
case class SparqlWriter(uri: String,
                        parameter: String = "query",
                        graphUri: String = "") extends LinkWriter {
  /**Maximum number of statements per request. */
  private val StatementsPerRequest = 200;

  private val log = Logger.getLogger(classOf[SparqlWriter].getName)

  private var connection: HttpURLConnection = null

  private var writer: Writer = null

  private var statements = 0

  override def open() {
    beginSparul(true)
  }

  override def write(link: Link, predicateUri: String) {
    if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    writeStatement(link, predicateUri)
  }

  override def close() {
    endSparql()
  }

  /**
   * Begins a new SPARQL/Update request.
   *
   * @param newGraph Create a new (empty) graph?
   * @throws IOException
   */
  private def beginSparul(newGraph: Boolean) {
    openConnection()
    if (graphUri.isEmpty) {
      writer.write("INSERT+DATA+%7B")
    }
    else {
      if (newGraph) {
        writer.write("CREATE+SILENT+GRAPH+%3C" + graphUri + "%3E+")
        closeConnection()
        openConnection()
      }
      writer.write("INSERT+DATA+INTO+%3C" + graphUri + "%3E+%7B")
    }
  }

  /**
   * Adds a link to the current SPARQL/Update request.
   *
   * @param nodes The statement
   * @throws IOException
   */
  private def writeStatement(link: Link, predicateUri: String) {
    writer.write(URLEncoder.encode("<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n", "UTF-8"))
    statements += 1
  }

  /**
   * Ends the current SPARQL/Update request.
   *
   * @throws IOException
   */
  private def endSparql() {
    writer.write("%7D")
    closeConnection()
  }

  private def openConnection() {
    //Preconditions
    require(connection == null, "Connectiom already openend")

    //Open a new HTTP connection
    val url = new URL(uri)
    connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    statements = 0;

    writer.write(parameter + "=")
  }

  private def closeConnection() {
    //Close connection
    writer.close()

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 == 2) {
      log.info(statements + " statements written to Store.")
    }
    else {
      val errorStream = connection.getErrorStream
      if (errorStream != null) {
        val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
        log.warning("SPARQL/Update query on " + uri + " failed. Error Message: '" + errorMessage + "'.")
      }
      else {
        log.warning("SPARQL/Update query on " + uri + " failed. Server response: " + connection.getResponseCode + " " + connection.getResponseMessage + ".")
      }
    }

    connection = null
    writer = null
  }
}