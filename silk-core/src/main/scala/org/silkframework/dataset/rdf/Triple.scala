package org.silkframework.dataset.rdf

/**
  * Represents an RDF Triple
  *
  * @param subject - subject (either Resource or BlankNode)
  * @param predicate - the predicate uri
  * @param objectVal - the object (as Resource, BlankNode or Literal)
  */
class Triple(
  subj: ConcreteNode,
  pred: Resource,
  objVal: RdfNode
) extends Quad(subj, pred, objVal, None)

object Triple{

  def apply(subject: ConcreteNode, predicate: Resource, obj: RdfNode): Triple = {
    new Triple(subject, predicate, obj)
  }
}