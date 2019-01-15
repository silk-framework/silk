package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.dataset.rdf.Quad

trait QuadFormatter {

  def formatQuad(quad: Quad): String

  def formatAsTriple(triple: Quad): String
}
