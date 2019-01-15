package org.silkframework.dataset.rdf

import org.silkframework.entity.Link

/**
 * Serializes a link.
 */
trait LinkFormatter extends Formatter {
  def formatLink(link: Link, predicate: String): String
}
