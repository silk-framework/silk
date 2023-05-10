package org.silkframework.dataset.rdf

import org.silkframework.runtime.iterator.CloseableIterator

import java.io.Closeable
import scala.collection.immutable.SortedMap

/**
  * SPARQL query results.
  * Has to be closed after usage.
  */
case class SparqlResults(variables: Seq[String], bindings: CloseableIterator[SortedMap[String, RdfNode]]) extends Closeable {

  override def close(): Unit = {
    bindings.close()
  }

}

object SparqlResults {

  def apply(bindings: Iterable[SortedMap[String, RdfNode]]): SparqlResults = {
    val variables: Seq[String] = if (bindings.isEmpty) Seq.empty else bindings.head.keys.toSeq
    SparqlResults(variables, CloseableIterator(bindings.iterator))
  }

}

class SparqlAskResult(val askResult: Boolean) extends SparqlResults(Seq.empty, CloseableIterator.empty)
