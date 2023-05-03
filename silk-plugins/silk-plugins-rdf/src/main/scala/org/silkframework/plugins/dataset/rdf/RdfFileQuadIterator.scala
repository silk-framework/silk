package org.silkframework.plugins.dataset.rdf

import java.io.{File, FileInputStream}

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.dataset.rdf.{Quad, QuadIterator}

/**
  * A [[QuadIterator]] based on a RDF file.
  */
case class RdfFileQuadIterator(rdfFile: File, lang: Lang) extends QuadIterator {
  private lazy val fis = new FileInputStream(rdfFile)
  private lazy val iter = RDFDataMgr.createIteratorQuads(fis, lang, null)

  override protected def closeResources(): Unit = fis.close()

  override def hasNext: Boolean = iter.hasNext

  override def next(): Quad = RdfFormatUtil.jenaQuadToQuad(iter.next())
}
