package org.silkframework.dataset.rdf

import java.io.{BufferedOutputStream, InputStream, OutputStream}
import java.net.{HttpURLConnection, SocketTimeoutException, URL, URLEncoder}
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.HttpURLConnectionUtils._

import scala.util.control.NonFatal

/**
 * Graph Store API trait.
 */
trait GraphStoreTrait {

  private val UNAUTHORIZED = 401

  def graphStoreEndpoint(graph: String): String

  /** HTTP headers to add to the graph store requests */
  def graphStoreHeaders(userContext: UserContext): Map[String, String] = Map.empty

  /**
    * Handles a connection error.
    * The default implementation throws a runtime exception that contains the error body as read from the connection.
    */
  def handleError(connection: HttpURLConnection, message: String): Nothing = {
    val serverErrorMessage = connection.errorMessage(prefix = " Error message: ").getOrElse("")
    throw new RuntimeException(message + s" Got ${connection.getResponseCode} response on ${connection.getURL}." + serverErrorMessage)
  }

  /** This is called if an authentication error (401) happened.
    * @return true if the authentication problem could be fixed, false otherwise. */
  def handleAuthenticationError(userContext: UserContext): Boolean = {
    false
  }

  def defaultTimeouts: GraphStoreDefaults = {
    val cfg = DefaultConfig.instance()
    val connectionTimeout = cfg.getInt("graphstore.default.connection.timeout.ms")
    val readTimeout = cfg.getInt("graphstore.default.read.timeout.ms")
    val maxRequestSize = cfg.getLong("graphstore.default.max.request.size")
    val fileUploadTimeout = cfg.getInt("graphstore.default.fileUpload.timeout.ms")
    GraphStoreDefaults(connectionTimeoutInMs = connectionTimeout, readTimeoutMs = readTimeout,
      maxRequestSize = maxRequestSize, fileUploadTimeoutInMs = fileUploadTimeout)
  }

  /**
   * Allows to write triples directly into a graph. The [[OutputStream]] must be closed by the caller.
    *
    * @param graph
   * @param contentType
   * @return A buffered output stream
   */
  def postDataToGraph(graph: String,
                      contentType: String = "application/n-triples",
                      chunkedStreamingMode: Option[Int] = Some(1000),
                      comment: Option[String] = None)
                     (implicit userContext: UserContext): OutputStream = {
    val connection: HttpURLConnection = initConnection(graph, comment)
    connection.setDoInput(true)
    connection.setDoOutput(true)
    chunkedStreamingMode foreach { connection.setChunkedStreamingMode }
    connection.setUseCaches(false)
    connection.setRequestProperty("Content-Type", contentType)
    // since the OutputStream is used externally we cannot do the authentication error handling here
    ConnectionClosingOutputStream(connection, userContext, errorHandler)
  }

  def deleteGraph(graph: String)
                 (implicit userContext: UserContext): Unit = {
    var tries = 0
    var success = false
    while(!success && tries < 2) {
      tries += 1
      val connection = initConnection(graph)
      connection.setRequestMethod("DELETE")
      success = connection.getResponseCode / 100 == 2
      if(!success) {
        if(tries == 1 && connection.getResponseCode == UNAUTHORIZED) {
          handleAuthenticationError(userContext)
        } else if(connection.getResponseCode / 100 == 5) {
          // Try again on server error
        } else {
          handleError(connection, s"Could not delete graph $graph!")
        }
      }
    }
  }

  private def initConnection(graph: String, comment: Option[String] = None)
                            (implicit userContext: UserContext): HttpURLConnection = {
    var graphStoreUrl = graphStoreEndpoint(graph)
    for(c <- comment) {
      graphStoreUrl += "&comment=" + URLEncoder.encode(c, "UTF8")
    }
    val url = new URL(graphStoreUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    for ((header, headerValue) <- graphStoreHeaders(userContext)) {
      connection.setRequestProperty(header, headerValue)
    }
    connection.setConnectTimeout(defaultTimeouts.connectionTimeoutInMs)
    connection.setReadTimeout(defaultTimeouts.readTimeoutMs)
    connection
  }

  private def errorHandler: ErrorHandler = ErrorHandler(handleError, handleAuthenticationError)

  def getDataFromGraph(graph: String,
                       acceptType: String = "text/turtle; charset=utf-8")
                      (implicit userContext: UserContext): InputStream = {
    def connection(): HttpURLConnection = {
      val c = initConnection(graph)
      c.setRequestMethod("GET")
      c.setDoInput(true)
      c.setRequestProperty("Accept", acceptType)
      c
    }
    ConnectionClosingInputStream(connection, userContext, errorHandler)
  }
}

/**
 * Handles the sending of the request and the closing of the connection on closing the [[OutputStream]].
 */
case class ConnectionClosingOutputStream(connection: HttpURLConnection,
                                         userContext: UserContext,
                                         errorHandler: ErrorHandler) extends OutputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private lazy val outputStream = {
    connection.connect()
    new BufferedOutputStream(connection.getOutputStream)
  }

  override def write(i: Int): Unit = {
    outputStream.write(i)
  }

  override def close(): Unit = {
    try {
      outputStream.close()
      val responseCode = connection.getResponseCode
      if(responseCode / 100 == 2) {
        log.fine("Successfully written to output stream.")
      } else {
        if(responseCode == 401) {
          errorHandler.authenticationErrorHandler(userContext) // Nothing else we can do here, all the data has already been written
        }
        errorHandler.genericErrorHandler(connection, s"Could not write to graph store. Got $responseCode response code.")
      }
    } catch {
      case _: SocketTimeoutException =>
        throw new RuntimeException("A read timeout has occurred during writing via the GraphStore protocol. " +
            s"You might want to increase 'graphstore.default.read.timeout.ms' in the application config. " +
            s"It is currently set to ${connection.getReadTimeout}ms.")
    } finally {
      connection.disconnect()
    }
  }
}

case class ConnectionClosingInputStream(createConnection: () => HttpURLConnection,
                                        userContext: UserContext,
                                        errorHandler: ErrorHandler) extends InputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  private var connection: HttpURLConnection = null

  private lazy val inputStream: InputStream = {
    var tries = 0
    var is: InputStream = null
    while(tries < 2) {
      tries += 1
      connection = createConnection()
      connection.connect()
      try {
        is = connection.getInputStream
      } catch {
        case NonFatal(_) =>
          if(tries == 1 && connection.getResponseCode == 401 && errorHandler.authenticationErrorHandler(userContext)) {
            // Authentication problem solved, let's try again
          } else {
            errorHandler.genericErrorHandler(connection, s"Could not read from graph store. Got ${connection.getResponseCode} response code.")
          }
      }
    }
    assert(is != null, "InputStream has not been initialized!")
    is
  }

  override def read(): Int = inputStream.read()

  override def close(): Unit = {
    try {
      inputStream.close()
      val responseCode = connection.getResponseCode
      if(responseCode / 100 == 2) {
        log.fine("Successfully received data from input stream.")
      } else {
        if(responseCode == 401) {
          errorHandler.authenticationErrorHandler(userContext)
        }
        errorHandler.genericErrorHandler(connection, s"Could not read from graph store. Got $responseCode response code.")
      }
    } finally {
      connection.disconnect()
    }
  }
}

case class GraphStoreDefaults(connectionTimeoutInMs: Int,
                              readTimeoutMs: Int,
                              maxRequestSize: Long,
                              fileUploadTimeoutInMs: Int)

case class ErrorHandler(genericErrorHandler: (HttpURLConnection, String) => Nothing,
                        authenticationErrorHandler: (UserContext) => Boolean = (_) => false)