package org.silkframework.plugins.dataset.rdf

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.rdf.{Quad, QuadIterator}
import org.silkframework.execution.ExecutionReportUpdater
import org.silkframework.execution.typed.QuadEntitySchema
import org.silkframework.runtime.plugin.PluginContext

import java.io.{File, FileInputStream}

/**
  * A [[QuadIterator]] based on a RDF file.
  */
case class RdfFileQuadIterator(rdfFile: File, lang: Lang, executionReportUpdater: ExecutionReportUpdater) extends QuadIterator {
  private lazy val fis = new FileInputStream(rdfFile)
  private lazy val iter = RDFDataMgr.createIteratorQuads(fis, lang, null)
  private var reporterInitialized = false
  implicit val prefixes: Prefixes = Prefixes.empty

  override protected def closeResources(): Unit = fis.close()

  override def hasNext: Boolean = iter.hasNext

  override def next(): Quad = {
    val quad = RdfFormatUtil.jenaQuadToQuad(iter.next())
    if(!reporterInitialized) {
      executionReportUpdater.startNewOutputSamples(QuadEntitySchema.schema)
      reporterInitialized = true
    }
    executionReportUpdater.addEntityAsSampleEntity(QuadEntitySchema.toEntity(quad)(PluginContext.empty))
    executionReportUpdater.increaseEntityCounter()
    quad
  }
}
