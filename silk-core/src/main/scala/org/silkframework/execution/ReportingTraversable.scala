package org.silkframework.execution

import org.silkframework.config.Prefixes
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{LocalEntities, MultiEntityTable}
import org.silkframework.runtime.iterator.CloseableIterator

import scala.util.control.NonFatal

/**
  * An entity iterable that forwards all entity traversals to an execution report.
  */
case class ReportingIterable(entities: Iterable[Entity])(implicit executionReport: ExecutionReportUpdater, prefixes: Prefixes) extends Iterable[Entity] {

  override def iterator: Iterator[Entity] = {
    ReportingIterator(CloseableIterator(entities.iterator))
  }
}

/**
  * An entity iterator that forwards all entity traversals to an execution report.
  */
case class ReportingIterator(entities: CloseableIterator[Entity])(implicit executionReport: ExecutionReportUpdater, prefixes: Prefixes) extends CloseableIterator[Entity] {

  override def hasNext: Boolean = {
    try {
      if (entities.hasNext) {
        true
      } else {
        executionReport.executionDone()
        false
      }
    } catch {
      case NonFatal(ex) =>
        executionReport.setExecutionError(Some(ex.getMessage))
        executionReport.executionDone()
        throw ex
    }
  }

  override def next(): Entity = {
    val entity =
      try {
        entities.next()
      } catch {
        case NonFatal(ex) =>
          executionReport.setExecutionError(Some(ex.getMessage))
          executionReport.executionDone()
          throw ex
      }
    executionReport.addSampleEntity(entity)
    executionReport.increaseEntityCounter()
    entity
  }

  override def close(): Unit = {
    entities.close()
  }
}

object ReportingIterator {

  /**
   * Adds a execution reporter to a local entities table and potential sub tables.
   */
  def addReporter(entities: LocalEntities)(implicit executionReport: ExecutionReportUpdater, prefixes: Prefixes): LocalEntities = {
    val reportingTraversable = ReportingIterator(CloseableIterator(entities.entities))(executionReport, prefixes)
    entities match {
      case multiTable: MultiEntityTable =>
        multiTable.copy(entities = reportingTraversable, subTables = multiTable.subTables.map(addReporter))
      case _ =>
        entities.updateEntities(reportingTraversable, entities.entitySchema)
    }
  }

}

