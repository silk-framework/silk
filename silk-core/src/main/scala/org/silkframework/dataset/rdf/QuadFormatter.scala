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
  def formatAsTriple(triple: Triple): String

  /**
    * Parse a given line into a Quad
    * @param txt - serialized Quad
    */
  def parseQuad(txt: String): Quad

  /**
    * Parse a given line into a Triple
    * @param txt - serialized Triple
    */
  def parseTriple(txt: String): Triple
}
