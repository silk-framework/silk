package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.output.{Formatter}
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Base trait of all formatters using XML.
 */
trait XMLFormatter extends Formatter {
  override final def format(link: Link, predicateUri: String): String = formatXML(link, predicateUri).toString + "\n"

  def formatXML(link: Link, predicateUri: String): NodeSeq
}