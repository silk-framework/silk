package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity
import org.silkframework.execution.local.TripleEntityTable
import org.silkframework.util.Uri

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
) extends Quad(subj, pred, objVal, None) {

  override def serialize(formatter: QuadFormatter): String = formatter.formatAsTriple(this)

  override def toEntity(uri: Option[Uri] = None): Entity ={
    val (value, typ) = TripleEntityTable.convertToEncodedType(this.objectVal)
    val values = IndexedSeq(
      Seq(this.subject.value),
      Seq(this.predicate.value),
      Seq(value),
      Seq(typ)
    )
    Entity(uri.getOrElse(Uri(values.head.head)), values, TripleEntityTable.schema)
  }
}

object Triple{

  val DefaultTripleContext = Resource("urn:instance:default:triple:context")

  def apply(subject: Resource, predicate: Resource, obj: RdfNode) = new Triple(subject, predicate, obj)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode) = new Triple(subject, predicate, obj)

}