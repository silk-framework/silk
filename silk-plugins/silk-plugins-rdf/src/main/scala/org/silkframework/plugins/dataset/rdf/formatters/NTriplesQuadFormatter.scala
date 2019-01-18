package org.silkframework.plugins.dataset.rdf.formatters

import java.io.ByteArrayOutputStream

import org.apache.jena.riot.RDFDataMgr
import org.silkframework.dataset.rdf.{Quad, _}
import org.silkframework.plugins.dataset.rdf.RdfFormatUtil

import scala.collection.JavaConverters._

class NTriplesQuadFormatter() extends QuadFormatter {

  private def format(quad: Quad, asQuad: Boolean = true): String = {
    val sos = new ByteArrayOutputStream()
    RDFDataMgr.writeQuads(sos, Iterator(RdfFormatUtil.quadToJenaQuad(quad)).asJava)
    sos.toString.dropRight(1)
  }

  override def formatQuad(quad: Quad): String = format(quad)

  override def formatAsTriple(triple: Quad): String = format(triple, asQuad = false)
}
