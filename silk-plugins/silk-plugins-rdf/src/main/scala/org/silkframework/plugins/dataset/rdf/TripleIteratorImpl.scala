package org.silkframework.plugins.dataset.rdf

import java.io.File

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{QuadFormatter, Triple, TripleIterator}
import org.silkframework.entity.Entity

import scala.io.Source

class TripleIteratorImpl(
  val hasTriple: () => Boolean,
  val nextTriple: () => Triple,
  val close: () => Unit = () => Unit,
  val formatter: QuadFormatter
) extends TripleIterator {

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  override def asEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( triple => {
      count += 1
      triple.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }

}

object TripleIteratorImpl{
  def apply(
   hasQuad: () => Boolean,
   nextQuad: () => Triple,
   close: () => Unit = () => Unit,
   formatter: QuadFormatter
 ): TripleIteratorImpl = {
    new TripleIteratorImpl(hasQuad, nextQuad, close, formatter)
  }

  def apply(file: File, formatter: QuadFormatter): TripleIteratorImpl = {
    val iter = Source.fromFile(file, "UTF-8").getLines()
    new TripleIteratorImpl(
      () => iter.hasNext,
      () => formatter.parseTriple(iter.next),
      () => Unit,
      formatter
    )
  }
}