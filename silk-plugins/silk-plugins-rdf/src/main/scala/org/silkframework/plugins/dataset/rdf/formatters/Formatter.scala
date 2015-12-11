package org.silkframework.plugins.dataset.rdf.formatters

/**
 * Created by andreas on 12/11/15.
 */
trait Formatter {
  def header: String = ""

  def footer: String = ""
}
