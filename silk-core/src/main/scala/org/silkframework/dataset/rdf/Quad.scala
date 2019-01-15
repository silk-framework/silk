package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity
import org.silkframework.execution.local.{QuadEntityTable, TripleEntityTable}
import org.silkframework.util.Uri

case class Quad private(
   subject: Either[Resource, BlankNode],
   predicate: Resource,
   objectVal: RdfNode,
   context: Option[Resource]
 ) {

  def toEntity(uri: Option[Uri] = None): Entity ={
    val (value, typ) = TripleEntityTable.convertToEncodedType(this.objectVal)
    val values = IndexedSeq(
      this.subject match{
        case Left(v) => Seq(v.value)
        case Right(v) => Seq(v.value)
      },
      Seq(this.predicate.value),
      Seq(value),
      Seq(typ),
      this.context.toSeq.map(_.value)
    )
    Entity(uri.getOrElse(Uri(values.head.head)), values, QuadEntityTable.schema)
  }

  private def replaceQuotes(value: String) = value.replaceAll("(^|[^\\\\])\"", "\\\\\"")

  def serialize(asQuad: Boolean = true, formatter: QuadFormatter = new NTriplesQuadFormatter): String = {
    if(asQuad) formatter.formatQuad(this)
    else formatter.formatAsTriple(this)
  }

  def serializeTriple(formatter: QuadFormatter = new NTriplesQuadFormatter): String = serialize(asQuad = false, formatter = formatter)

  override def toString: String = this.serialize()
}

object Quad{

  def apply(subject: Resource, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Left(subject), predicate, obj, context)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Right(subject), predicate, obj, context)

  def triple(subject: Resource, predicate: Resource, obj: RdfNode) = new Quad(Left(subject), predicate, obj, None)
  def triple(subject: BlankNode, predicate: Resource, obj: RdfNode) = new Quad(Right(subject), predicate, obj, None)

}
