package de.fuberlin.wiwiss.silk.util.sparql

import xml.{XML, Elem}
import java.util.logging.{Level, Logger}
import io.Source
import java.io.IOException
import java.net._

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
class RemoteSparqlEndpoint(val uri : URI,
                           login : Option[(String, String)] = None,
                           val pageSize : Int = 1000, val pauseTime : Int = 0,
                           val retryCount : Int = 3, val initialRetryPause : Int = 1000) extends SparqlEndpoint
{
  private val logger = Logger.getLogger(classOf[RemoteSparqlEndpoint].getName)

  private var lastQueryTime = 0L

  override def toString = "SparqlEndpoint(" + uri + ")"

  override def query(sparql : String, limit : Int) : Traversable[Map[String, Node]] = new ResultTraversable(sparql, limit)

  private class ResultTraversable(sparql : String, limit : Int) extends Traversable[Map[String, Node]]
  {
    override def foreach[U](f : Map[String, Node] => U) : Unit =
    {
      var blankNodeCount = 0

      for(offset <- 0 until limit by pageSize)
      {
        val xml = executeQuery(sparql + " OFFSET " + offset + " LIMIT " + math.min(pageSize, limit - offset))

        val resultsXml = xml \ "results" \ "result"

        for(resultXml <- resultsXml)
        {
          val values = for(binding <- resultXml \ "binding"; node <- binding \ "_") yield node.label match
          {
            case "uri" => (binding \ "@name" text, Resource(node.text))
            case "literal" => (binding \ "@name" text, Literal(node.text))
            case "bnode" =>
            {
              blankNodeCount += 1
              (binding \ "@name" text, BlankNode("bnode" + blankNodeCount))
            }
          }

          f(values.toMap)
        }

        if(resultsXml.size < pageSize) return
      }
    }

    /**
     * Executes a SPARQL SELECT query.
     *
     * @param query The SPARQL query to be executed
     * @return Query result in SPARQL Query Results XML Format
     */
    private def executeQuery(query : String) : Elem =
    {
      //Wait until pause time is elapsed since last query
      synchronized
      {
        while(System.currentTimeMillis < lastQueryTime + pauseTime) Thread.sleep(pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }

      //Execute query
      if(logger.isLoggable(Level.FINE)) logger.fine("Executing query on " + uri +"\n" + query)

      val url = new URL(uri + "?format=application/sparql-results%2Bxml&query=" + URLEncoder.encode(query, "UTF-8"))

      var result : Elem = null
      var retries = 0
      var retryPause = initialRetryPause
      while(result == null)
      {
        val httpConnection = RemoteSparqlEndpoint.openConnection(url, login)

        try
        {
          result = XML.load(httpConnection.getInputStream)
        }
        catch
        {
          case ex : IOException =>
          {
            retries += 1
            if(retries > retryCount)
            {
              throw ex
            }

            if(logger.isLoggable(Level.INFO))
            {
              val errorStream = httpConnection.getErrorStream
              if(errorStream != null)
              {
                val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
                logger.info("Query on " + uri + " failed:\n" + query + "\nError Message: '" + errorMessage + "'.\nRetrying in " + retryPause + " ms. (" + retries + "/" + retryCount + ")")
              }
              else
              {
                logger.info("Query on " + uri + " failed:\n" + query + "\nRetrying in " + retryPause + " ms. (" + retries + "/" + retryCount + ")")
              }
            }

            Thread.sleep(retryPause)
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          }
          case ex : Exception =>
          {
            logger.log(Level.SEVERE, "Could not execute query on " + uri + ":\n" + query, ex)
            throw ex
          }
        }
      }

      //Return result
      if(logger.isLoggable(Level.FINE)) logger.fine("Query Result\n" + result)
      result
    }
  }
}

private object RemoteSparqlEndpoint
{
  /**
   * Opens a new HTTP connection to the endpoint.
   * This method is synchronized to avoid race conditions as the Authentication is set globally in Java.
   */
  private def openConnection(url : URL, login : Option[(String, String)]) : HttpURLConnection = synchronized
  {
    //Set authentication
    for((user, password) <- login)
    {
      Authenticator.setDefault(new Authenticator()
      {
        override def getPasswordAuthentication = new PasswordAuthentication(user, password.toCharArray)
      })
    }

    //Open connection
    val httpConnection = url.openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")

    httpConnection
  }
}
