package org.silkframework.dataset.rdf

import org.silkframework.util.CloseableIterator

import scala.collection.immutable.SortedMap

case class SparqlResults(variables: Seq[String], bindings: CloseableIterator[SortedMap[String, RdfNode]])

object SparqlResults {

  def apply(bindings: Iterable[SortedMap[String, RdfNode]]): SparqlResults = {
    val variables: Seq[String] = if (bindings.isEmpty) Seq.empty else bindings.head.keys.toSeq
    SparqlResults(variables, CloseableIterator(bindings.iterator))
  }

}

class SparqlAskResult(val askResult: Boolean) extends SparqlResults(Seq.empty, CloseableIterator.empty)
