package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity
import org.silkframework.execution.local.{QuadEntityTable, TripleEntityTable}
import org.silkframework.util.Uri

/**
  * Represents an RDF Quad
  * @param subject - subject (either Resource or BlankNode)
  * @param predicate - the predicate uri
  * @param objectVal - the object (as Resource, BlankNode or Literal)
  * @param context - the graph or context (either Resource or BlankNode)
  */
case class Quad (
   subject: ConcreteNode,
   predicate: Resource,
   objectVal: RdfNode,
   context: ConcreteNode    // note no blank nodes allowed as context
 ) {

  def toEntity(uri: Option[Uri] = None): Entity ={
    assert(Triple.DefaultTripleContext != context, "Trying to extract Quad-Entities from a Triple")
    val (value, typ) = TripleEntityTable.convertToEncodedType(this.objectVal)
    val values = IndexedSeq(
      Seq(this.subject.value),
      Seq(this.predicate.value),
      Seq(value),
      Seq(typ),
      Seq(this.context.value)
    )
    Entity(uri.getOrElse(Uri(values.head.head)), values, QuadEntityTable.schema)
  }

  def serialize(formatter: QuadFormatter): String = formatter.formatQuad(this)

}

object Quad{

  def apply(subject: Resource, predicate: Resource, obj: RdfNode, context: Resource) = new Quad(subject, predicate, obj, context)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode, context: Resource) = new Quad(subject, predicate, obj, context)

}
