package org.silkframework.workbench.utils

import java.io.InputStream

import org.silkframework.runtime.validation.RequestException
import play.api.libs.json.{JsString, JsValue, Json}

import scala.io.Source
import scala.util.control.NonFatal

/**
  * Error response according to RFC 7807 "Problem Details for HTTP APIs".
  */
case class HttpProblemDetailsException(errorTitle: String,
                                       detail: String,
                                       cause: Option[HttpProblemDetailsException],
                                       httpErrorCode: Option[Int] = None) extends RequestException(detail, cause)

object HttpProblemDetailsException {

  /**
    * Parses our version of RFC 7807.
    *
    * @param inputStream The input stream to read from. Will be closed on return.
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

  def parse(json: JsValue): HttpProblemDetailsException = {
    val title = (json \ "title").as[JsString].value
    val detail = (json \ "detail").as[JsString].value
    val cause = (json \ "cause").toOption.map(parse)
    new HttpProblemDetailsException(title, detail, cause)
  }

}
