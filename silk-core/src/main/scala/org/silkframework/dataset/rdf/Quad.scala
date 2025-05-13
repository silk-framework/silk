package org.silkframework.dataset.rdf

/**
  * Represents an RDF Quad
  * @param subject - subject (either Resource or BlankNode)
  * @param predicate - the predicate uri
  * @param objectVal - the object (as Resource, BlankNode or Literal)
  * @param context - the optional graph
  */
case class Quad (
   subject: ConcreteNode,
   predicate: Resource,
   objectVal: RdfNode,
   context: Option[Resource] = None
 )

object Quad{

  def apply(subject: ConcreteNode, predicate: Resource, obj: RdfNode, context: Resource): Quad = {
    new Quad(subject, predicate, obj, Some(context))
  }
}
