package org.silkframework.plugins.dataset.rdf

import org.apache.jena.query.QueryExecution
import org.silkframework.dataset.rdf.{Triple, QuadIterator}

/**
  * A triple iterator based on [[org.apache.jena.query.QueryExecution]]
  */
case class QueryExecutionTripleIterator(queryExecution: QueryExecution) extends QuadIterator {
  private val tripleIterator = queryExecution.execConstructTriples()

  override protected def closeResources(): Unit = queryExecution.close()

  override def hasNext: Boolean = tripleIterator.hasNext

  override def next(): Triple = RdfFormatUtil.jenaTripleToTriple(tripleIterator.next())
}
