package org.silkframework.plugins.dataset.rdf.datasets

import java.io.{File, FileInputStream}

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.QuadEntityTable
import org.silkframework.plugins.dataset.rdf.formatters.NTriplesQuadFormatter
import org.silkframework.plugins.dataset.rdf.{QuadIteratorImpl, RdfFormatUtil}

/**
  * Extends QuadEntityTable based on a RDF file, which is always reloaded when entities is called
  * @param file - the rdf file containing quads
  * @param task - the pertaining task
  */
class FileBasedQuadEntityTable(file: File, task: Task[TaskSpec]) extends QuadEntityTable(() => {
  val sos = new FileInputStream(file)
  val iter = RDFDataMgr.createIteratorQuads(sos, Lang.NQUADS, null)
  new QuadIteratorImpl(
    () => iter.hasNext,
    () => RdfFormatUtil.jenaQuadToQuad(iter.next()),
    () => sos.close(),
    new NTriplesQuadFormatter
  ).asEntities},
  task)
