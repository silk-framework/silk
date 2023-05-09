package org.silkframework.execution

import org.silkframework.entity.Entity
import org.silkframework.runtime.iterator.CloseableIterator

import scala.util.control.NonFatal

/**
  * An entity iterable that forwards all entity traversals to an execution report.
  */
case class ReportingIterable(entities: Iterable[Entity])(implicit executionReport: ExecutionReportUpdater) extends Iterable[Entity] {

  override def iterator: Iterator[Entity] = {
    ReportingIterator(CloseableIterator(entities.iterator))
  }
}

case class ReportingIterator(entities: CloseableIterator[Entity])(implicit executionReport: ExecutionReportUpdater) extends CloseableIterator[Entity] {

  override def hasNext: Boolean = {
    if(entities.hasNext) {
      true
    } else {
      executionReport.executionDone()
      false
    }
  }

  override def next(): Entity = {
    val entity =
      try {
        entities.next()
      } catch {
        case NonFatal(ex) =>
          executionReport.setExecutionError(Some(ex.getMessage))
          throw ex
      }
    executionReport.increaseEntityCounter()
    entity
  }

  override def close(): Unit = {
    entities.close()
  }
}

