package de.fuberlin.wiwiss.silk.util

import java.net.{URL, URLEncoder}
import xml.{XML, Elem}
import java.util.logging.{Level, Logger}
import java.io.IOException

/**
 * Executes queries on a SPARQL endpoint.
 *
 * @param uri The URI of the endpoint
 * @param pauseTime The minimum number of milliseconds between two queries
 * @param retryCount The number of retries if a query fails
 * @param retryPause The pause in milliseconds before a query is retried.
 */
class SparqlEndpoint(val uri : String, val pauseTime : Int = 0, val retryCount : Int = 3, val retryPause : Int = 1000)
{
    private val logger = Logger.getLogger(classOf[SparqlEndpoint].getName)

    private var lastQueryTime = 0L

    /**
     * Executes a SPARQL SELECT query.
     *
     * @param query The SPARQL query to be executed
     * @return Query result in SPARQL Query Results XML Format 
     */
    def query(query : String) : Elem =
    {
        //Wait until pause time is elapsed since last query
        synchronized
        {
            while(System.currentTimeMillis < lastQueryTime + pauseTime) Thread.sleep(pauseTime / 10)
            lastQueryTime = System.currentTimeMillis
        }

        //Execute query
        if(logger.isLoggable(Level.FINE)) logger.fine("Executing query on " + uri +"\n" + query)

        var result : Elem = null
        var retries = 0
        while(result == null)
        {
            try
            {
                result = XML.load(uri + "?format=application/rdf+xml&query=" + URLEncoder.encode(query, "UTF-8"))
            }
            catch
            {
                case ex : Exception =>
                {
                    retries += 1
                    if(retries > retryCount) throw ex
                    if(logger.isLoggable(Level.INFO)) logger.info("Query failed:\n" + query + "\nError: '" + ex.getMessage + "'. Retrying in " + retryPause + " ms.")
                    Thread.sleep(retryPause)
                }
            }
        }

        //Return result
        if(logger.isLoggable(Level.FINE)) logger.fine("Query Result\n" + result)
        result
    }
}
