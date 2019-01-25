package org.silkframework.plugins.dataset.rdf.formatters

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.dataset.rdf.{Quad, _}
import org.silkframework.plugins.dataset.rdf.RdfFormatUtil

import scala.collection.JavaConverters._

class NTriplesQuadFormatter() extends QuadFormatter {

  private def format(quad: Quad, asQuad: Boolean = true): String = {
    val sos = new ByteArrayOutputStream()
    if(asQuad)
      RDFDataMgr.writeQuads(sos, Iterator(RdfFormatUtil.quadToJenaQuad(quad)).asJava)
    else
      RDFDataMgr.writeTriples(sos, Iterator(RdfFormatUtil.quadToJenaQuad(quad).asTriple()).asJava)
    sos.toString.dropRight(1)
  }

  override def formatQuad(quad: Quad): String = format(quad)

  override def formatAsTriple(triple: Triple): String = format(triple, asQuad = false)

  private def parse(quad: String, asQuad: Boolean = true): Quad = {
    val sos = new ByteArrayInputStream(quad.getBytes)
    if(asQuad){
      val iter = RDFDataMgr.createIteratorQuads(sos, Lang.NQUADS, null)
      if(iter.hasNext) {
        val jenaQuad = iter.next()
        RdfFormatUtil.jenaQuadToQuad(jenaQuad)
      }
      else
        null
    }
    else{
      val iter = RDFDataMgr.createIteratorTriples(sos, Lang.NTRIPLES, null)
      if(iter.hasNext) {
        val jenaTriple = iter.next()
        RdfFormatUtil.jenaTripleToTriple(jenaTriple)
      }
      else
        null
    }
  }

  override def parseQuad(txt: String): Quad = parse(txt)

  override def parseTriple(txt: String): Triple = parse(txt, asQuad = false).asInstanceOf[Triple]

  /**
    * The pertaining html media type
    */
  override def associatedMediaType: String = Lang.NQUADS.getContentType.getContentType
}
