package org.silkframework.dataset.rdf

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.FileResource
import org.silkframework.util.HttpURLConnectionUtils._

import java.io._
import java.net.{HttpURLConnection, SocketTimeoutException, URL, URLEncoder}
import java.nio.file.Files
import java.util.logging.Logger
import scala.util.Try
import scala.util.control.NonFatal

/**
 * Graph Store API trait.
 */
trait GraphStoreTrait {

  private val UNAUTHORIZED = 401

  protected val log: Logger = Logger.getLogger(getClass.getName)

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
    this match {
      case uploadGraphStore: GraphStoreFileUploadTrait =>
        // If file upload is available, use different approach. GSP file upload is much more stable and usually offers a retry mechanism.
        postDataToGraphViaFileUpload(uploadGraphStore, graph, contentType, comment)
      case _ =>
        postDataToGraphViaPostRequest(graph, contentType, chunkedStreamingMode, comment)
    }
  }

  private def postDataToGraphViaFileUpload(fileUploadGraphStore: GraphStoreFileUploadTrait,
                                           graph: String,
                                           contentType: String,
                                           comment: Option[String])
                                          (implicit userContext: UserContext): OutputStream = {
    GraphStoreUploadOutputStream(
      fileUploadGraphStore,
      graph = graph,
      contentType = contentType,
      comment = comment,
      userContext = userContext
    )
  }

  private def postDataToGraphViaPostRequest(graph: String,
                                            contentType: String = "application/n-triples",
                                            chunkedStreamingMode: Option[Int],
                                            comment: Option[String])
                                           (implicit userContext: UserContext): OutputStream = {
    log.fine("Initiating Graph Store POST request to: " + graph + comment.map(c => s" ($c)").getOrElse(""))

    val connection: HttpURLConnection = initConnection(graph, comment)
    connection.setDoInput(true)
    connection.setDoOutput(true)
    chunkedStreamingMode foreach { connection.setChunkedStreamingMode }
    connection.setUseCaches(false)
    connection.setRequestProperty("Content-Type", contentType)
    // since the OutputStream is used externally we cannot do the authentication error handling here
    ConnectionClosingOutputStream(connection, userContext, errorHandler)
  }

  def deleteGraph(graph: String,
                 ignoreIfNotExists: Boolean = false)
                (implicit userContext: UserContext): Unit = {
    log.fine(s"Deleting graph '$graph' from Graph Store")
    var tries = 0
    var success = false
    while(!success && tries < 2) {
      tries += 1
      GraphStoreTrait.handleTimeoutErrors(defaultTimeouts.readTimeoutMs, s"Deleting graph '$graph' has failed: ") {
        val connection = initConnection(graph)
        connection.setRequestMethod("DELETE")
        val responseCode = connection.getResponseCode
        success = responseCode / 100 == 2
        if (!success) {
          if (ignoreIfNotExists && responseCode == 404) {
            log.fine(s"Graph $graph does not exist and cannot be deleted. Ignoring as requested.")
            return
          }
          if (tries == 1 && responseCode == UNAUTHORIZED) {
            handleAuthenticationError(userContext)
            log.fine(s"Request to delete graph $graph has been successful.")
          } else if (responseCode / 100 == 5) {
            // Try again on server error
          } else {
            handleError(connection, s"Could not delete graph $graph!")
          }
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
                       acceptType: String = "application/n-triples; charset=utf-8")
                      (implicit userContext: UserContext): InputStream = {
    log.fine(s"Initiating Graph Store GET request to: $graph")
    def connection(): HttpURLConnection = {
      val c = initConnection(graph)
      c.setRequestMethod("GET")
      c.setDoInput(true)
      c.setRequestProperty("Accept", acceptType)
      c
    }
    ConnectionClosingInputStream(connection, userContext, errorHandler, s"$graph from Graph Store")
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
      GraphStoreTrait.handleTimeoutErrors(connection.getReadTimeout) {
        outputStream.close()
        val responseCode = connection.getResponseCode
        if(responseCode / 100 == 2) {
          log.fine("Successfully written to graph store output stream.")
        } else {
          if(responseCode == 401) {
            errorHandler.authenticationErrorHandler(userContext) // Nothing else we can do here, all the data has already been written
          }
          errorHandler.genericErrorHandler(connection, s"Could not write to graph store. Got $responseCode response code.")
        }
      }
    } finally {
      connection.disconnect()
    }
  }
}

private object GraphStoreTrait {
  // Handle socket read timeouts
  def handleTimeoutErrors[T](readTimeoutMs: Int, errorPrefix: String = "")(block: => T): T = {
    try {
      block
    } catch {
      case ex: SocketTimeoutException =>
        throw new GraphStoreException(errorPrefix + "A read timeout has occurred during writing via the GraphStore protocol. " +
          s"You might want to increase 'graphstore.default.read.timeout.ms' in the application config. " +
          s"It is currently set to ${readTimeoutMs}ms.", ex)
    }
  }
}

private class GraphStoreException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

case class ConnectionClosingInputStream(createConnection: () => HttpURLConnection,
                                        userContext: UserContext,
                                        errorHandler: ErrorHandler,
                                        sourceName: String) extends InputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  @volatile
  private var connection: HttpURLConnection = null

  private lazy val inputStream: InputStream = {
    var tries = 0
    var is: InputStream = null
    while(is == null && tries < 2) {
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
            errorHandler.genericErrorHandler(connection, s"Could not read $sourceName. Got ${connection.getResponseCode} response code.")
          }
      }
    }
    assert(is != null, "InputStream has not been initialized!")
    is
  }

  override def read(): Int = inputStream.read()

  override def read(b: Array[Byte], off: Int, len: Int): Int = inputStream.read(b, off, len)

  override def read(b: Array[Byte]): Int = inputStream.read(b)

  override def readAllBytes(): Array[Byte] = inputStream.readAllBytes()

  override def readNBytes(b: Array[Byte], off: Int, len: Int): Int = inputStream.readNBytes(b, off, len)

  override def readNBytes(len: Int): Array[Byte] = inputStream.readNBytes(len)

  override def skip(n: Long): Long = inputStream.skip(n)

  override def close(): Unit = {
    if(connection != null) {
      try {
        inputStream.close()
        val responseCode = connection.getResponseCode
        if (responseCode / 100 == 2) {
          log.fine(s"Successfully received $sourceName")
        } else {
          if (responseCode == 401) {
            errorHandler.authenticationErrorHandler(userContext)
          }
          errorHandler.genericErrorHandler(connection, s"Could not read $sourceName. Got $responseCode response code.")
        }
      } finally {
        connection.disconnect()
        connection = null
      }
    }
  }
}

/** Output stream that writes to a local temporary file and uploads the file via the graph store file upload on close.
  * This uses potential retry mechanisms of the file upload method and keeps the duration of the connection low. */
case class GraphStoreUploadOutputStream(fileUploadGraphStore: GraphStoreFileUploadTrait,
                                        graph: String,
                                        contentType: String,
                                        comment: Option[String],
                                        userContext: UserContext) extends OutputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  implicit private val uc: UserContext = userContext

  @volatile
  private var initialized = false
  private var fileResource: Option[FileResource] = None

  private lazy val tempFile: File = {
    val file = Files.createTempFile("graphStoreUpload", ".nt").toFile
    file.deleteOnExit()
    // Make sure this file is also deleted if the output stream is not correctly closed
    val resource = FileResource(file)
    resource.setDeleteOnGC(true)
    fileResource = Some(resource)
    initialized = true
    file
  }

  private lazy val outputStream = {
    log.fine("Created temporary file for GSP file upload.")
    new BufferedOutputStream(new FileOutputStream(tempFile))
  }

  override def write(i: Int): Unit = {
    outputStream.write(i)
  }

  override def close(): Unit = {
    try {
      if (initialized) {
        outputStream.flush()
        outputStream.close()
        if(tempFile.exists() && tempFile.isFile && tempFile.length() > 0) {
          fileUploadGraphStore.uploadFileToGraph(graph, tempFile, contentType, comment)
        }
      }
    } finally {
      Try(tempFile.delete())
    }
  }
}

case class GraphStoreDefaults(connectionTimeoutInMs: Int,
                              readTimeoutMs: Int,
                              maxRequestSize: Long,
                              fileUploadTimeoutInMs: Int)

case class ErrorHandler(genericErrorHandler: (HttpURLConnection, String) => Nothing,
                        authenticationErrorHandler: (UserContext) => Boolean = (_) => false)