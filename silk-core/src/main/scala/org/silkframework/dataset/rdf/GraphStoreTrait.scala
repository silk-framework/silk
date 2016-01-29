package org.silkframework.dataset.rdf

import java.io.OutputStream
import java.net.{HttpURLConnection, URL}
import java.util.logging.Logger

import scala.util.Try

/**
 * Created by andreas on 1/28/16.
 */
trait GraphStoreTrait {
  def graphStoreEndpoint(graph: String): String

  /**
   * Allows to write triples directly into a graph. The [[OutputStream]] must be closed by the caller.
   * @param graph
   * @param contentType
   * @return
   */
  def postDataToGraph(graph: String,
                      contentType: String = "application/n-triples",
                      chunkedStreamingMode: Option[Int] = Some(1000)): OutputStream = {
    val updateUrl = graphStoreEndpoint(graph)
    val url = new URL(updateUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoInput(true)
    connection.setDoOutput(true)
    chunkedStreamingMode foreach { connection.setChunkedStreamingMode(_) }
    connection.setUseCaches(false)
    connection.setRequestProperty("Content-Type", contentType)
    ConnectionClosingOutputStream(connection)
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
      log.fine("Finished with code " + connection.getResponseCode)
    } finally {
      connection.disconnect()
    }
  }
}