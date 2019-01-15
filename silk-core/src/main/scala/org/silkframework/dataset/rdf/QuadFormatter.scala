package org.silkframework.dataset.rdf

/**
  * Serializes Quads
  */
trait QuadFormatter extends Formatter{

  /**
    * Serializes a [Quad] with context (if present).
    */
  def formatQuad(quad: Quad): String

  /**
    * Serializes a [Quad] as a triple without context.
    */
  def formatAsTriple(triple: Quad): String
}
