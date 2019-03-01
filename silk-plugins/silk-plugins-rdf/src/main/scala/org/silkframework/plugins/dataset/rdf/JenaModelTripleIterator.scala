package org.silkframework.plugins.dataset.rdf

import org.apache.jena.rdf.model.Model
import org.silkframework.dataset.rdf.{Triple, TripleIterator}

/**
  * A quad iterator that iterates over all triples of the given [[Model]] .
  */
case class JenaModelTripleIterator(model: Model) extends TripleIterator {
  private val iterator = model.listStatements()

  override protected def closeResources(): Unit = iterator.close()

  override def hasNext: Boolean = iterator.hasNext

  override def next(): Triple = RdfFormatUtil.jenaStatementToTriple(iterator.next())
}