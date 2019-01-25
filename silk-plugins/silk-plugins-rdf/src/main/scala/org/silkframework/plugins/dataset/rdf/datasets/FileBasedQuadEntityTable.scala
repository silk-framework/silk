package org.silkframework.plugins.dataset.rdf.datasets

import java.io.{File, FileInputStream}

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.rdf.QuadFormatter
import org.silkframework.execution.local.QuadEntityTable
import org.silkframework.plugins.dataset.rdf.{QuadIteratorImpl, RdfFormatUtil}

/**
  * Extends QuadEntityTable based on a RDF file, which is always reloaded when entities is called
  * @param file - the rdf file containing quads
  * @param task - the pertaining task
  */
class FileBasedQuadEntityTable(file: File, lang: Lang, task: Task[TaskSpec]) extends QuadEntityTable(() => {
  val sos = new FileInputStream(file)
  val iter = RDFDataMgr.createIteratorQuads(sos, lang, null)
  new QuadIteratorImpl(
    () => iter.hasNext,
    () => RdfFormatUtil.jenaQuadToQuad(iter.next()),
    () => sos.close(),
    QuadFormatter.getSuitableFormatter(lang.getContentType.getContentType).getOrElse(throw new IllegalArgumentException("Unknown media type: " + lang.getContentType.toString))
  ).asEntities},
  task)
