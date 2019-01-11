package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{QuadEntityTable, TripleEntityTable}

/**
  * Abstracts the quad interface of a construct query result as Iterator
 *
  * @param hasQuad - function indicating if there exists an additional quad
  * @param nextQuad - function retrieving the next quad
  * @param reset - function to reset the iterator to its origin (unused) state
  */
case class QuadIterator(
   hasQuad: () => Boolean,
   nextQuad: () => Quad,
   reset: () => Unit
 ) extends Iterator[Quad] {

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = nextQuad()

  override def toString(): String = {
    val sb = new StringBuilder()
    while(hasNext){
      val quad = next()
      // subject
      quad.subject match{
        case Left(r) => sb.append("<").append(r.value).append("> ")
        case Right(b) => sb.append("_:").append(b.value).append(" ")   //TODO check blank node syntax
      }
      // predicate
      sb.append("<").append(quad.predicate.value).append("> ")
      // object
      quad.objectVal match{
        case Resource(value) => sb.append("<").append(value).append("> ")
        case BlankNode(value) => sb.append("_:").append(value).append(" ")   //TODO check blank node syntax
        case LanguageLiteral(value, lang) => sb.append("\"").append(value).append("\"@").append(lang).append(" ")
        case DataTypeLiteral(value, typ) => sb.append("\"").append(value).append("\"^^").append(typ).append(" ")
        case PlainLiteral(value) => sb.append("\"").append(value).append("\" ")
      }
      // graph
      if(quad.context.nonEmpty){
        sb.append("<").append(quad.context.get).append("> ")
      }
      // line end
      sb.append(".\n")
    }
    //reset iterator
    reset()
    // to string
    sb.toString()
  }

  def getQuadEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      val (value, typ) = TripleEntityTable.convertToEncodedType(quad.objectVal)
      val values = IndexedSeq(
        quad.subject match{
          case Left(v) => Seq(v.value)
          case Right(v) => Seq(v.value)
        },
        Seq(quad.predicate.value),
        Seq(value),
        Seq(typ),
        quad.context.toSeq.map(_.value)
      )
      count += 1
      Entity(DataSource.URN_NID_PREFIX + count, values, QuadEntityTable.schema)
    })
  }
}
