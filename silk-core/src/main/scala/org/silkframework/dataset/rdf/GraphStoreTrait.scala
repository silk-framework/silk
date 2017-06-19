package org.silkframework.dataset.rdf

import java.io.{InputStream, OutputStream}
import java.net.{HttpURLConnection, SocketTimeoutException, URL}
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig

/**
 * Graph Store API trait.
 */
trait GraphStoreTrait {
  def graphStoreEndpoint(graph: String): String

  def graphStoreHeaders(): Map[String, String] = Map.empty

  def defaultTimeouts: GraphStoreTimeouts = {
    val cfg = DefaultConfig.instance()
    val connectionTimeout = cfg.getInt("graphstore.default.connection.timeout.ms")
    val readTimeout = cfg.getInt("graphstore.default.read.timeout.ms")
    GraphStoreTimeouts(connectionTimeoutIsMs = connectionTimeout, readTimeoutMs = readTimeout)
  }

  /**
   * Allows to write triples directly into a graph. The [[OutputStream]] must be closed by the caller.
    *
    * @param graph
   * @param contentType
   * @return
   */
  def postDataToGraph(graph: String,
                      contentType: String = "application/n-triples",
                      chunkedStreamingMode: Option[Int] = Some(1000)): OutputStream = {
    val connection: HttpURLConnection = initConnection(graph)
    connection.setDoInput(true)
    connection.setDoOutput(true)
    chunkedStreamingMode foreach { connection.setChunkedStreamingMode }
    connection.setUseCaches(false)
    connection.setRequestProperty("Content-Type", contentType)
    ConnectionClosingOutputStream(connection)
  }

  private def initConnection(graph: String): HttpURLConnection = {
    val graphStoreUrl = graphStoreEndpoint(graph)
    val url = new URL(graphStoreUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    for ((header, headerValue) <- graphStoreHeaders()) {
      connection.setRequestProperty(header, headerValue)
    }
    connection.setConnectTimeout(defaultTimeouts.connectionTimeoutIsMs)
    connection.setReadTimeout(defaultTimeouts.readTimeoutMs)
    connection
  }

  def getDataFromGraph(graph: String,
                       acceptType: String = "text/turtle; charset=utf-8"): InputStream = {
    val connection: HttpURLConnection = initConnection(graph)
    connection.setRequestMethod("GET")
    connection.setDoInput(true)
    connection.setRequestProperty("Accept", acceptType)
    ConnectionClosingInputStream(connection)
  }
}

/**
 * Handles the sending of the request and the closing of the connection on closing the [[OutputStream]].
 */
case class ConnectionClosingOutputStream(connection: HttpURLConnection) extends OutputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private lazy val outputStream = {
    connection.connect()
    connection.getOutputStream()
  }

  override def write(i: Int): Unit = {
    outputStream.write(i)
  }

  override def close(): Unit = {
    try {
      outputStream.flush()
      outputStream.close()
      val responseCode = connection.getResponseCode
      if(responseCode / 100 == 2) {
        log.fine("Successfully written to output stream.")
      } else {
        throw new RuntimeException(s"Could not write to HTTP connection. Got $responseCode response code. Message: ${connection.getResponseMessage}")
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

case class ConnectionClosingInputStream(connection: HttpURLConnection) extends InputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private lazy val inputStream: InputStream = {
    connection.connect()
    connection.getInputStream
  }

  override def read(): Int = inputStream.read()

  override def close(): Unit = {
    try {
      inputStream.close()
      val responseCode = connection.getResponseCode
      if(responseCode / 100 == 2) {
        log.fine("Successfully received data from input stream.")
      } else {
        throw new RuntimeException(s"Could not read from HTTP connection. Got $responseCode response code. Message: ${connection.getResponseMessage}")
      }
    } finally {
      connection.disconnect()
    }
  }
}

case class GraphStoreTimeouts(connectionTimeoutIsMs: Int, readTimeoutMs: Int)