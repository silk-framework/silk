package org.silkframework.util

import java.net.HttpURLConnection

import scala.io.Source

/**
  * Helper methods for [[java.net.HttpURLConnection]]
  */
object HttpURLConnectionUtils {
  /** Retrieves the error message for this connection for a failed request. */
  def errorMessage(connection: HttpURLConnection, prefix: String = ""): Option[String] = {
    val errorStreamOpt = Option(connection.getErrorStream)
    errorStreamOpt map { errorStream =>
      prefix + Source.fromInputStream(errorStream).getLines.mkString("\n")
    }
  }
}
