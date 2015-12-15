package org.silkframework.plugins.dataset.rdf.formatters

/**
 * Created by andreas on 12/11/15.
 */
trait EntityFormatter extends Formatter {
  def formatLiteralStatement(subject: String, predicate: String, value: String): String
}
