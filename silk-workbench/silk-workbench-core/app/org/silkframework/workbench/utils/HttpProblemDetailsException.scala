package org.silkframework.workbench.utils

import java.io.InputStream

import play.api.libs.json.{JsString, JsValue, Json}

import scala.io.Source
import scala.util.control.NonFatal

/**
  * Error response according to RFC 7807 "Problem Details for HTTP APIs".
  */
case class HttpProblemDetailsException(title: String, detail: String, cause: Option[HttpProblemDetailsException]) extends Exception(detail, cause.orNull)

object HttpProblemDetailsException {

  /**
    * Parses our version of RFC 7807.
    *
    * @param inputStream The input stream to read from. Will be close on return.
    */
  def parse(inputStream: InputStream): HttpProblemDetailsException = {
    val str = Source.fromInputStream(inputStream, "UTF8").getLines().mkString("\n")
    try {
      parse(Json.parse(str))
    } catch {
      case NonFatal(ex) =>
        new HttpProblemDetailsException("Server response could not be parsed", str, None)
    } finally {
      inputStream.close()
    }
  }

  private def parse(json: JsValue): HttpProblemDetailsException = {
    val title = (json \ "title").as[JsString].value
    val detail = (json \ "detail").as[JsString].value
    val cause = (json \ "cause").toOption.map(parse)
    new HttpProblemDetailsException(title, detail, cause)
  }

}
