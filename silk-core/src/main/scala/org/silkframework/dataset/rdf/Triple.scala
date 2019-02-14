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

  def toTripleEntity(uri: Option[Uri] = None): Entity = {
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

  def apply(subject: ConcreteNode, predicate: Resource, obj: RdfNode): Triple = {
    new Triple(subject, predicate, obj)
  }
}