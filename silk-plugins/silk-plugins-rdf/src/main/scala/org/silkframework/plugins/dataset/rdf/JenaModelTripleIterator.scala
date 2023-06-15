package org.silkframework.plugins.dataset.rdf

import org.apache.jena.rdf.model.Model
import org.silkframework.dataset.rdf.{Triple, TripleIterator}

/**
  * A quad iterator that iterates over all triples of the given [[Model]] .
  */
case class JenaModelTripleIterator(model: Model) extends TripleIterator {

  private val iter = model.listStatements()

  override protected def closeResources(): Unit = iter.close()

  override def hasNext: Boolean = iter.hasNext

  override def next(): Triple = RdfFormatUtil.jenaStatementToTriple(iter.next())
}