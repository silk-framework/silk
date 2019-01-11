package org.silkframework.dataset.rdf

case class Quad private(
   subject: Either[Resource, BlankNode],
   predicate: Resource,
   objectVal: RdfNode,
   context: Option[Resource]
 ) {

}

object Quad{

  def apply(subject: Resource, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Left(subject), predicate, obj, context)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Right(subject), predicate, obj, context)

  def triple(subject: Resource, predicate: Resource, obj: RdfNode) = new Quad(Left(subject), predicate, obj, None)
  def triple(subject: BlankNode, predicate: Resource, obj: RdfNode) = new Quad(Right(subject), predicate, obj, None)

}