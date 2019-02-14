package org.silkframework.plugins.dataset.rdf.datasets

import java.io.File

import org.apache.jena.riot.Lang
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.QuadEntityTable
import org.silkframework.plugins.dataset.rdf.RdfFileQuadIterator

/**
  * Extends QuadEntityTable based on a RDF file, which is always reloaded when entities is called
  * @param file - the rdf file containing quads
  * @param task - the pertaining task
  */
class FileBasedQuadEntityTable(file: File, lang: Lang, task: Task[TaskSpec]) extends
    QuadEntityTable(
      entityFunction = () => RdfFileQuadIterator(file, lang).asQuadEntities,
      task = task
    )
