package org.silkframework.plugins.dataset.rdf

import org.apache.jena.query.QueryExecution
import org.silkframework.dataset.rdf.{Quad, QuadIterator}

/**
  * A quad iterator based on [[org.apache.jena.query.QueryExecution]]
  */
case class QueryExecutionQuadIterator(queryExecution: QueryExecution) extends QuadIterator {
  private val quadIterator = queryExecution.execConstructQuads()

  override protected def closeResources(): Unit = queryExecution.close()

  override def hasNext: Boolean = quadIterator.hasNext

  override def next(): Quad = RdfFormatUtil.jenaQuadToQuad(quadIterator.next())
}
