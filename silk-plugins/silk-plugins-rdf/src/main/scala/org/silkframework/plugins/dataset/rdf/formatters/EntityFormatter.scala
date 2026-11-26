package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.dataset.rdf.Formatter
import org.silkframework.entity.ValueType

import org.silkframework.entity.ValueType

/**
 * Created by andreas on 12/11/15.
 */
trait EntityFormatter extends Formatter {
  def formatLiteralStatement(subject: String, predicate: String, value: String, valueType: ValueType): String

  def formatLiteralStatement(subject: String,
                             predicate: String,
                             value: String,
                             subjectType: ValueType,
                             valueType: ValueType): String = {
    formatLiteralStatement(subject, predicate, value, valueType)
  }
}
