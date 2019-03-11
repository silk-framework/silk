package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity
import org.silkframework.execution.local.{QuadEntityTable, TripleEntityTable}
import org.silkframework.util.Uri

/**
  * Represents an RDF Quad
  * @param subject - subject (either Resource or BlankNode)
  * @param predicate - the predicate uri
  * @param objectVal - the object (as Resource, BlankNode or Literal)
  * @param context - the optional graph or context (either Resource or BlankNode)
  */
case class Quad (
   subject: ConcreteNode,
   predicate: Resource,
   objectVal: RdfNode,
   context: Option[Resource]    // note no blank nodes allowed as context
 ) {

  def toQuadEntity(uri: Option[Uri] = None): Entity = {
    val (value, typ) = TripleEntityTable.convertToEncodedType(this.objectVal)
    val values = IndexedSeq(
      Seq(this.subject.value),
      Seq(this.predicate.value),
      Seq(value),
      Seq(typ),
      this.context.map(_.value).toSeq
    )
    Entity(uri.getOrElse(Uri(values.head.head)), values, QuadEntityTable.schema)
  }
}

object Quad{

  def apply(subject: ConcreteNode, predicate: Resource, obj: RdfNode, context: Resource): Quad = {
    new Quad(subject, predicate, obj, Some(context))
  }
}
