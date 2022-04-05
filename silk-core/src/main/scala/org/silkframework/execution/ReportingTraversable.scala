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
