package org.silkframework.plugins.dataset.rdf

import java.net.URI

import org.silkframework.util.StringUtils
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.util.Try

/**
  * Created on 8/31/16.
  */
object RdfFormatUtil {
  def tripleValuesToNTriplesSyntax(subject: String, property: String, value: String): String = {
    value match {
      // Check if value is an URI
      case v if value.startsWith("http") && Try(URI.create(value)).isSuccess =>
        "<" + subject + "> <" + property + "> <" + v + "> ."
      // Check if value is a number
      case StringUtils.integerNumber() =>
        "<" + subject + "> <" + property + "> \"" + value + "\"^^<http://www.w3.org/2001/XMLSchema#integer> ."
      case DoubleLiteral(d) =>
        "<" + subject + "> <" + property + "> \"" + d + "\"^^<http://www.w3.org/2001/XMLSchema#double> ."
      // Write string values
      case _ =>
        val escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        "<" + subject + "> <" + property + "> \"" + escapedValue + "\" ."
    }
  }
}
