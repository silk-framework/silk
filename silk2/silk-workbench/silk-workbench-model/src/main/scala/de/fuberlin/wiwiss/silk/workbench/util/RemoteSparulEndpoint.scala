
package de.fuberlin.wiwiss.silk.workbench.util

import java.util.logging.{Level, Logger}
import java.io.IOException
import java.net._
import org.apache.http.client.methods.HttpPost
import org.apache.http.protocol.HTTP
import org.apache.http.client.HttpClient
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.scheme.{PlainSocketFactory,Scheme,SchemeRegistry}
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.params.{BasicHttpParams, CoreProtocolPNames, HttpProtocolParams }
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.HttpVersion


/**
 * Executes queries on a remote SPARQL\Update endpoint - via HTTPPOST
 *
 * @param uri The URI of the endpoint
 * @param login The login required by the endpoint for authentication
 */

class RemoteSparulEndpoint(val uri : URI, val prefixes : Map[String, String],
                           login : Option[(String, String)] = None  )
{
  private val logger = Logger.getLogger(classOf[RemoteSparulEndpoint].getName)

  private val sparqlPrefixes = prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

  override def toString = "SparulEndpoint(" + uri + ")"

  def query(sparul : String) = executeQuery(sparqlPrefixes + sparul)

  private def executeQuery(query : String) = {

      logger.info("Executing query on " + uri +"\n" + query)

      val httppost = new HttpPost(uri)
      val formParams =  new java.util.ArrayList[BasicNameValuePair]
      // TODO - use InputStream
      formParams.add(new BasicNameValuePair("command", query))
      httppost.setEntity(new UrlEncodedFormEntity(formParams, HTTP.UTF_8));

      val httpClient = HttpClientFactory.createHttpClient

      try{
        val response = httpClient.execute(httppost);
      }
      catch {

        case ex : IOException =>  {
             logger.log(Level.SEVERE, "Query on " + uri + " failed:\n" + query,ex)
             throw ex
            }

        case ex : Exception =>  {
              logger.log(Level.SEVERE, "Could not execute query on " + uri + ":\n" + query, ex)
              throw ex
            }
      }

  }
}


private object RemoteSparulEndpoint    {
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


/**
 * Helper class to create org.apache.http.client.HttpClient with sensible defaults
 */
private object HttpClientFactory {

	val MAX_CONNECTIONS = 100;
  val HTTP_USER_AGENT = "ldimporter/0.1 (http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/index.html)";
  val HTTP_SOCKET_TIMEOUT = 30 * 1000;

	def createHttpClient : HttpClient = {
	    /* Create and initialize HTTP parameters */
	    val params = new BasicHttpParams()
	    ConnManagerParams.setMaxTotalConnections(params, MAX_CONNECTIONS)
	    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1)
		  params.setIntParameter("http.connection.timeout", HTTP_SOCKET_TIMEOUT)
		  params.setParameter(CoreProtocolPNames.USER_AGENT, HTTP_USER_AGENT)

	    /* Create and initialize scheme registry */
	    val schemeRegistry = new SchemeRegistry()
	    schemeRegistry.register( new Scheme("http", PlainSocketFactory.getSocketFactory(), 80))

	    /*
	     * Create an HttpClient with the ThreadSafeClientConnManager.
	     * This connection manager must be used if more than one thread will
	     * be using the HttpClient.
	     */
	    val cm : ClientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry)

      new DefaultHttpClient(cm, params)
	}
}
