package de.fuberlin.wiwiss.silk.util.sparql

import xml.{XML, Elem}
import java.util.logging.{Level, Logger}
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.io.IOException
import io.Source

/**
 * Executes queries on a SPARQL endpoint.
 *
 * @param uri The URI of the endpoint
 * @param pageSize The number of solutions to be retrieved per SPARQL query (default: 1000)
 * @param pauseTime The minimum number of milliseconds between two queries
 * @param retryCount The number of retries if a query fails
 * @param retryPause The pause in milliseconds before a query is retried.
 */
class SparqlEndpoint(val uri : String, val pageSize : Int = 1000, val pauseTime : Int = 0, val retryCount : Int = 3, val retryPause : Int = 1000)
{
    private val logger = Logger.getLogger(classOf[SparqlEndpoint].getName)

    private var lastQueryTime = 0L

    def query(sparql : String) : Traversable[Map[String, Node]] = new ResultTraversable(sparql)

    private class ResultTraversable(sparql : String) extends Traversable[Map[String, Node]]
    {
        override def foreach[U](f : Map[String, Node] => U) : Unit =
        {
            for(offset <- 0 until Integer.MAX_VALUE by pageSize)
            {
                val xml = executeQuery(sparql + " OFFSET " + offset + " LIMIT " + pageSize)

                val resultsXml = xml \ "results" \ "result"

                for(resultXml <- resultsXml)
                {
                    val values = for(binding <- resultXml \ "binding"; node <- binding \ "_") yield node.label match
                    {
                        case "uri" => (binding \ "@name" text, new Resource(node.text))
                        case "literal" => (binding \ "@name" text, new Literal(node.text))
                        case label => throw new RuntimeException("Unsupported element: <" + label + "> in SPARQL result binding")
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

            val url = new URL(uri + "?format=application/rdf+xml&query=" + URLEncoder.encode(query, "UTF-8"))

            var result : Elem = null
            var retries = 0
            while(result == null)
            {
                val httpConnection = url.openConnection.asInstanceOf[HttpURLConnection]

                try
                {
                    result = XML.load(httpConnection.getInputStream)
                }
                catch
                {
                    case ex : IOException =>
                    {
                        retries += 1
                        if(retries > retryCount) throw ex

                        if(logger.isLoggable(Level.INFO))
                        {
                            val errorStream = httpConnection.getErrorStream
                            if(errorStream != null)
                            {
                                val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
                                logger.info("Query failed:\n" + query + "\nError Message: '" + errorMessage + "'.\nRetrying in " + retryPause + " ms.")
                            }
                            else
                            {
                                logger.info("Query failed.\nRetrying in " + retryPause + " ms.")
                            }
                        }

                        Thread.sleep(retryPause)
                    }
                }
            }

            //Return result
            if(logger.isLoggable(Level.FINE)) logger.fine("Query Result\n" + result)
            result
        }

//    private def doPostQuery(query : String) =
//    {
//        val encodedQuery = "?format=application/rdf+xml&query=" + URLEncoder.encode(query, "UTF-8")
//
//        val url = new URL(uri)
//        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
//        connection.setRequestMethod("POST")
//        connection.setDoOutput(true)
//
//        val outputStream = connection.getOutputStream
//        try
//        {
//            outputStream.write(encodedQuery.getBytes("UTF-8"))
//        }
//        finally
//        {
//            outputStream.close()
//        }
//
//        XML.load(connection.getInputStream)
//    }
    }
}
