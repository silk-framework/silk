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

  def serialize(asQuad: Boolean = true, formatter: QuadFormatter): String = {
    if(asQuad) formatter.formatQuad(this)
    else formatter.formatAsTriple(this)
  }

  def serializeTriple(formatter: QuadFormatter): String = serialize(asQuad = false, formatter = formatter)

  override def hashCode(): Int = {
    val prime = 31
    var hashCode =  this.subject match{
      case Left(v) => v.value.hashCode
      case Right(v) => v.value.hashCode
    }
    hashCode = hashCode * prime + this.predicate.value.hashCode()
    hashCode = hashCode * prime + this.objectVal.value.hashCode()
    this.context.foreach(c =>
      hashCode = hashCode * prime + c.value.hashCode()
    )
    hashCode
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match{
      case q: Quad =>
        val equalSubj = this.subject match{
          case Left(v) => q.subject.isLeft && q.subject.left.get.value == v.value
          case Right(v) => q.subject.isRight && q.subject.right.get.value == v.value
        }
        val equalPred = this.predicate.value == q.predicate.value
        val equalObj = this.objectVal.value == q.objectVal.value
        val equalContext = this.context match{
          case Some(c) => q.context.nonEmpty && q.context.get.value == c.value
          case None => q.context.isEmpty
        }
        equalSubj && equalPred && equalObj && equalContext
      case _ => false
    }
  }
}

object Quad{

  def apply(subject: Resource, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Left(subject), predicate, obj, context)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Right(subject), predicate, obj, context)

  def triple(subject: Resource, predicate: Resource, obj: RdfNode) = new Quad(Left(subject), predicate, obj, None)
  def triple(subject: BlankNode, predicate: Resource, obj: RdfNode) = new Quad(Right(subject), predicate, obj, None)

}
