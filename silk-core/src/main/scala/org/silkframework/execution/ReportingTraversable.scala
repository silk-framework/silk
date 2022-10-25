package org.silkframework.execution

import org.silkframework.entity.Entity

import scala.util.control.NonFatal

/**
  * An entity traversable that forwards all entity traversals to an execution report.
  */
case class ReportingTraversable(entities: Traversable[Entity])(implicit executionReport: ExecutionReportUpdater) extends Traversable[Entity] {
  override def foreach[U](f: Entity => U): Unit = {
    try {
      for(entity <- entities) {
        f(entity)
        executionReport.increaseEntityCounter()
      }
    } catch {
      case NonFatal(ex) =>
        executionReport.setExecutionError(Some(ex.getMessage))
        throw ex
    } finally {
      executionReport.executionDone()
    }
  }
}

case class ReportingIterable(entities: Iterable[Entity])(implicit executionReport: ExecutionReportUpdater) extends Iterable[Entity] {

  override def iterator: Iterator[Entity] = {
    ReportingIterator(entities.iterator)
  }
}

case class ReportingIterator(entities: Iterator[Entity])(implicit executionReport: ExecutionReportUpdater) extends Iterator[Entity] {

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
}

