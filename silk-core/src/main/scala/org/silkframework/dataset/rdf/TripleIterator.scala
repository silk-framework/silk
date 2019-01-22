package org.silkframework.dataset.rdf

trait TripleIterator extends QuadIterator with Iterator[Triple] {

  /**
    * function to indicate that the TripleIterator is or is not empty
    */
  val hasTriple: () => Boolean

  /**
    * Function for provisioning of the next Triple
    */
  val nextTriple: () => Triple

  /**
    * function to indicate that the QuadIterator is or is not empty
    */
  override val hasQuad: () => Boolean = hasTriple
  /**
    * Function for provisioning of the next Quad
    */
  override val nextQuad: () => Quad = nextTriple

  /**
    * Providing a [[TripleIterator]] by ignoring the context of each Quad
    */
  override def asTriples: TripleIterator = this

  override def hasNext: Boolean = hasTriple()

  override def next(): Triple = nextTriple()
}
