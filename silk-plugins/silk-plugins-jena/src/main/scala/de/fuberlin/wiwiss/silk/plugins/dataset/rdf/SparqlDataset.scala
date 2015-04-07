package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import java.io.{IOException, OutputStreamWriter, Writer}
import java.net._
import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.dataset.rdf.RdfDatasetPlugin
import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Link, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql._
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

import scala.io.Source

/**
 * Dataset which retrieves all entities from a SPARQL endpoint
 *
 * Parameters:
 * - '''endpointURI''': The URI of the SPARQL endpoint e.g. http://dbpedia.org/sparql
 * - '''login (optional)''': Login required for authentication
 * - '''password (optional)''': Password required for authentication
 * - '''graph (optional)''': Only retrieve entities from a specific graph
 * - '''pageSize (optional)''': The number of solutions to be retrieved per SPARQL query (default: 1000)
 * - '''entityList (optional)''': A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by a space.
 * - '''pauseTime (optional)''': The number of milliseconds to wait between subsequent query
 * - '''retryCount (optional)''': The number of retires if a query fails
 * - '''retryPause (optional)''': The number of milliseconds to wait until a failed query is retried
 * - '''queryParameters (optional)''' Additional parameters to be appended to every request e.g. &soft-limit=1
 * - '''parallel (optional)''' True (default), if multiple queries should be executed in parallel for faster retrieval.
 * - '''updateParameter (optional)''' The HTTP parameter used to submit queries. Defaults to "query".
 */
@Plugin(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "Dataset which retrieves all entities from a SPARQL endpoint")
case class SparqlDataset(endpointURI: String, login: String = null, password: String = null,
                         graph: String = null, pageSize: Int = 1000, entityList: String = null,
                         pauseTime: Int = 0, retryCount: Int = 3, retryPause: Int = 1000,
                         queryParameters: String = "", parallel: Boolean = true, updateParameter: String = "query") extends RdfDatasetPlugin {

  private val log = Logger.getLogger(SparqlDataset.getClass.getName)

  private val loginComplete = {
    if (login != null) {
      require(password != null, "No password provided for login '" + login + "'. Please set the 'password' parameter.")
      Some((login, password))
    } else {
      None
    }
  }

  override def sparqlEndpoint = {
    //new JenaRemoteEndpoint(endpointURI)
    new RemoteSparqlEndpoint(new URI(endpointURI), loginComplete, pageSize, pauseTime, retryCount, retryPause, queryParameters)
  }

  override def source = SparqlSource

  override def sink = SparqlSink

  object SparqlSource extends DataSource {

    private val graphUri = if (graph == null) None else Some(graph)

    private val entityUris = Option(entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

    override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
      val entityRetriever =
        if(parallel)
          new ParallelEntityRetriever(sparqlEndpoint, pageSize, graphUri)
        else
          new SimpleEntityRetriever(sparqlEndpoint, pageSize, graphUri)

      entityRetriever.retrieve(entityDesc, entityUris union entities)
    }

    override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
      //Create an endpoint which fails after 3 retries
      val failFastEndpoint = new RemoteSparqlEndpoint(new URI(endpointURI), loginComplete, pageSize, pauseTime, 3, 1000, queryParameters)

      try {
        SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
      } catch {
        case ex: Exception =>
          log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
          SparqlSamplePathsCollector(sparqlEndpoint, restrictions, limit)
      }
    }

    override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
      SparqlTypesCollector(sparqlEndpoint, limit)
    }

    override def toString = endpointURI
  }

  object SparqlSink extends DataSink {

    /**Maximum number of statements per request. */
    private val StatementsPerRequest = 200

    private val loginComplete = {
      if (login != null) {
        require(password != null, "No password provided for login '" + login + "'. Please set the 'password' parameter.")
        Some((login, password))
      } else {
        None
      }
    }

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
      else if (value.forall(c => c.isDigit || c == '.'))
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
      if (graph.isEmpty) {
        writer.write("INSERT+DATA+%7B")
      }
      else {
        if (newGraph) {
          writer.write("CREATE+SILENT+GRAPH+%3C" + graph + "%3E+")
          closeConnection()
          openConnection()
        }
        writer.write("INSERT+DATA+INTO+%3C" + graph + "%3E+%7B")
      }
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
      require(connection == null, "Connection already opened")

      //Set authentication
      for ((user, password) <- loginComplete) {
        Authenticator.setDefault(new Authenticator() {
          override def getPasswordAuthentication = new PasswordAuthentication(user, password.toCharArray)
        })
      }

      //Open a new HTTP connection
      val url = new URL(endpointURI)
      connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
      statements = 0

      writer.write(updateParameter + "=")
    }

    private def closeConnection() {
      // Close connection
      val con = connection
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
          throw new IOException("SPARQL/Update query on " + endpointURI + " failed. Error Message: '" + errorMessage + "'.")
        }
        else {
          throw new IOException("SPARQL/Update query on " + endpointURI + " failed. Server response: " + con.getResponseCode + " " + con.getResponseMessage + ".")
        }
      }
    }
  }

}
