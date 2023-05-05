package org.silkframework.dataset.rdf

import org.silkframework.util.CloseableIterator

import scala.collection.immutable.SortedMap

case class SparqlResults(bindings: CloseableIterator[SortedMap[String, RdfNode]]) {

  def variables: Seq[String] =
    if(bindings.isEmpty) Seq.empty
    else bindings.head.keys.toSeq
}

object SparqlResults {

  def apply(bindings: Iterable[SortedMap[String, RdfNode]]): SparqlResults = {
    SparqlResults(CloseableIterator(bindings.iterator))
  }

}

class SparqlAskResult(val askResult: Boolean) extends SparqlResults(CloseableIterator.empty)
