package org.silkframework.util

import java.io.OutputStreamWriter
import java.net.HttpURLConnection

import scala.io.Source

/**
  * Helper methods for [[java.net.HttpURLConnection]]
  */
object HttpURLConnectionUtils {

  implicit class HttpURLConnectionUtils(connection: HttpURLConnection) {

    /** Retrieves the error message for this connection for a failed request. */
    def errorMessage(prefix: String = ""): Option[String] = {
      val errorStreamOpt = Option(connection.getErrorStream)
      errorStreamOpt map { errorStream =>
        prefix + Source.fromInputStream(errorStream).getLines.mkString("\n")
      }
    }

    def setBody(body: String): Unit = {
      connection.setDoOutput(true)
      val writer = new OutputStreamWriter(connection.getOutputStream)
      try {
        writer.write(body)
      } finally {
        writer.close()
      }
    }

    def getResponseBody: String = {
      val inputStream = connection.getInputStream
      try {
        Source.fromInputStream(inputStream, "UTF8").getLines.mkString("\n")
      } finally {
        inputStream.close()
      }
    }

  }
}
