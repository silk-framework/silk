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

  def serialize(asQuad: Boolean = true/* FIXME, formatter: QuadFormatter = new NTriplesQuadFormatter*/): String = {
    // FIXME duplicate code with NTriplesQuadFormatter, the RDF interface should be moved out of silk core
    val sb = new StringBuilder()
    this.subject match{
      case Left(r) => sb.append("<").append(r.value).append("> ")
      case Right(b) => sb.append("_:").append(b.value).append(" ")
    }
    // predicate
    sb.append("<").append(this.predicate.value).append("> ")
    // object
    this.objectVal match{
      case Resource(value) => sb.append("<").append(value).append("> ")
      case BlankNode(value) => sb.append("_:").append(value).append(" ")
      case LanguageLiteral(value, lang) =>
        sb.append("\"").append(replaceQuotes(value))
        if(lang != null && lang.nonEmpty) sb.append("\"@").append(lang).append(" ")
        else sb.append("\" ")
      case DataTypeLiteral(value, typ) =>
        sb.append("\"").append(replaceQuotes(value))
        if(typ != null && typ.nonEmpty) sb.append("\"^^<").append(typ).append("> ")
        else sb.append("\" ")
      case PlainLiteral(value) =>
        sb.append("\"").append(replaceQuotes(value)).append("\" ")
    }
    // graph
    if(asQuad && this.context.nonEmpty){
      sb.append("<").append(this.context.get.value).append("> ")
    }
    // line end
    sb.append(". ")

    sb.toString()
  }

  def serializeTriple(): String = serialize(asQuad = false)

  override def toString: String = this.serialize()
}

object Quad{

  def apply(subject: Resource, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Left(subject), predicate, obj, context)
  def apply(subject: BlankNode, predicate: Resource, obj: RdfNode, context: Option[Resource]) = new Quad(Right(subject), predicate, obj, context)

  def triple(subject: Resource, predicate: Resource, obj: RdfNode) = new Quad(Left(subject), predicate, obj, None)
  def triple(subject: BlankNode, predicate: Resource, obj: RdfNode) = new Quad(Right(subject), predicate, obj, None)

}
