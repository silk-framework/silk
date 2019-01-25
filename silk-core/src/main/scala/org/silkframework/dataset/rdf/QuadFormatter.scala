package org.silkframework.dataset.rdf

import org.silkframework.util.ScalaReflectUtils

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

  /**
    * The pertaining html media type
    */
  def associatedMediaType: String

  // we make sure each formatter has a simple constructor
  assert(this.getClass.getConstructor() != null, "QuadFormatter without public, simple constructor was found, this is not allowed!")
}

object QuadFormatter{

  private lazy val implementingClasses = ScalaReflectUtils.getAllClassesImplementingTrait(QuadFormatter.getClass).map(c => {
    val constructor = c.getConstructor()
    val instance = constructor.newInstance().asInstanceOf[QuadFormatter]
    (instance.associatedMediaType, () => constructor.newInstance())
  }).toMap

  /**
    * Will create a new suitable QuadFormatter for the given media type (if available)
    * @param mediaType - the mediatype as String
    */
  def getSuitableFormatter(mediaType: String): Option[QuadFormatter] = implementingClasses.get(mediaType.trim)
    .map(_.apply().asInstanceOf[QuadFormatter])

}
