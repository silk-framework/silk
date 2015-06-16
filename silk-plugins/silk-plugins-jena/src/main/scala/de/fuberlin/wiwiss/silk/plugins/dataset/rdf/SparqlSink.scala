package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import java.io.{OutputStreamWriter, IOException, Writer}
import java.net._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.dataset.DataSink
import de.fuberlin.wiwiss.silk.entity.Link
import scala.io.Source

/**
 * A sink for writing to SPARQL/Update endpoints.
 */
class SparqlSink(params: SparqlParams) extends DataSink {

  private val log = Logger.getLogger(classOf[SparqlSink].getName)

  /**Maximum number of statements per request. */
  private val StatementsPerRequest = 200

  private var connection: HttpURLConnection = null

  private var writer: Writer = null

  private var statements = 0

  private var properties = Seq[String]()

  override def open(properties: Seq[String]) {
    this.properties = properties
  }

  override def writeLink(link: Link, predicateUri: String) {
    if(connection == null) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    writer.write(URLEncoder.encode("<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n", "UTF-8"))
    statements += 1
  }

  override def writeEntity(subject: String, values: Seq[Set[String]]) {
    if(connection == null) {
      beginSparul(true)
    } else if (statements + 1 > StatementsPerRequest) {
      endSparql()
      beginSparul(false)
    }

    for((property, valueSet) <- properties zip values; value <- valueSet) {
      writeStatement(subject, property, value)
    }
  }

  override def close() {
    if(connection != null) {
      endSparql()
    }
  }

  private def writeStatement(subject: String, property: String, value: String): Unit = {
    // Check if value is an URI
    if (value.startsWith("http:"))
      writer.write(URLEncoder.encode("<" + subject + "> <" + property + "> <" + value + "> .\n", "UTF-8"))
    // Check if value is a number
    else if (value.nonEmpty && value.forall(c => c.isDigit || c == '.'))
      writer.write(URLEncoder.encode("<" + subject + "> <" + property + "> \"" + value + "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n", "UTF-8"))
    // Write string values
    else {
      val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
      writer.write(URLEncoder.encode("<" + subject + "> <" + property + "> \"" + escapedValue + "\" .\n", "UTF-8"))
    }

    statements += 1
  }

  /**
   * Begins a new SPARQL/Update request.
   *
   * @param newGraph Create a new (empty) graph?
   * @throws IOException
   */
  private def beginSparul(newGraph: Boolean) {
    openConnection()
    if (params.graph.isEmpty) {
      writer.write("INSERT+DATA+%7B")
    }
    else {
      if (newGraph) {
        writer.write("CREATE+SILENT+GRAPH+%3C" + params.graph + "%3E+")
        closeConnection()
        openConnection()
      }
      writer.write("INSERT+DATA+INTO+%3C" + params.graph + "%3E+%7B")
    }
  }

  /**
   * Ends the current SPARQL/Update request.
   *
   * @throws IOException
   */
  private def endSparql() {
    if(writer != null)
      writer.write("%7D")
    closeConnection()
  }

  private def openConnection() {
    //Preconditions
    require(connection == null, "Connection already opened")

    //Set authentication
    for ((user, password) <- params.login) {
      Authenticator.setDefault(new Authenticator() {
        override def getPasswordAuthentication = new PasswordAuthentication(user, password.toCharArray)
      })
    }

    //Open a new HTTP connection
    val url = new URL(params.uri)
    connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    statements = 0

    writer.write(params.updateParameter + "=")
  }

  private def closeConnection() {
    // Close connection
    val con = connection
    if(writer != null)
      writer.close()
    writer = null
    connection = null

    //Check if the HTTP response code is in the range 2xx
    if (con.getResponseCode / 100 == 2) {
      log.info(statements + " statements written to Store.")
    }
    else {
      val errorStream = con.getErrorStream
      if (errorStream != null) {
        val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
        throw new IOException("SPARQL/Update query on " + params.uri + " failed. Error Message: '" + errorMessage + "'.")
      }
      else {
        throw new IOException("SPARQL/Update query on " + params.uri + " failed. Server response: " + con.getResponseCode + " " + con.getResponseMessage + ".")
      }
    }
  }
}
