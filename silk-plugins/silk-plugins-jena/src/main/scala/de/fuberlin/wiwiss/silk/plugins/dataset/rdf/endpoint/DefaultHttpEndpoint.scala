package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint

import java.io.{OutputStreamWriter, IOException}
import java.net.{HttpURLConnection, URL}
import javax.xml.bind.DatatypeConverter

import scala.io.Source
import scala.xml.{XML, Elem}

class DefaultHttpEndpoint extends HttpEndpoint {

  def select(url: String, login: Option[(String, String)]): Elem = {
    //Open connection
    val httpConnection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
    httpConnection.setRequestProperty("ACCEPT", "application/sparql-results+xml")
    //Set authentication
    for ((user, password) <- login) {
      httpConnection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes))
    }

    try {
      val inputStream = httpConnection.getInputStream
      val result = XML.load(inputStream)
      inputStream.close()
      result
    } catch {
      case ex: IOException =>
        val errorStream = httpConnection.getErrorStream
        if (errorStream != null) {
          val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
          throw new IOException(errorMessage, ex)
        } else {
          throw ex
        }
    } finally {
      httpConnection.disconnect()
    }
  }

  def update(url: String, body: String): Unit = {
    //Open a new HTTP connection
    val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    val writer = new OutputStreamWriter(connection.getOutputStream, "UTF-8")
    writer.write(body)
    writer.close()

    //Check if the HTTP response code is in the range 2xx
    if (connection.getResponseCode / 100 != 2) {
      val errorStream = connection.getErrorStream
      if (errorStream != null) {
        val errorMessage = Source.fromInputStream(errorStream).getLines.mkString("\n")
        throw new IOException("SPARQL/Update query on " + url + " failed with error code " + connection.getResponseCode + ". Error Message: '" + errorMessage + "'.")
      }
      else {
        throw new IOException("SPARQL/Update query on " + url + " failed. Server response: " + connection.getResponseCode + " " + connection.getResponseMessage + ".")
      }
    }
  }

}
