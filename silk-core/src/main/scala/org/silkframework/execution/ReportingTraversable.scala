package org.silkframework.execution

import org.silkframework.config.Prefixes
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{LocalEntities, MultiEntityTable}
import org.silkframework.execution.typed.TypedEntities
import org.silkframework.runtime.iterator.CloseableIterator

import scala.util.Try
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
 * An iterator that forwards all traversals to an execution report.
 */
class GenericReportingIterator[T](entities: CloseableIterator[T])(implicit executionReport: ExecutionReportUpdater) extends CloseableIterator[T] {

  @volatile
  private var closed = false

  /**
   * May be overridden to add sample entities to the execution report.
   */
  protected def addEntitySample(entity: T): Unit = { }

  override def hasNext: Boolean = {
    try {
      if (entities.hasNext) {
        true
      } else {
        close()
        false
      }
    } catch {
      case NonFatal(ex) =>
        executionReport.setExecutionError(Some(ex.getMessage))
        Try(close())
        throw ex
    }
  }

  override def next(): T = {
    val entity =
      try {
        entities.next()
      } catch {
        case NonFatal(ex) =>
          executionReport.setExecutionError(Some(ex.getMessage))
          executionReport.executionDone()
          throw ex
      }
    addEntitySample(entity)
    executionReport.increaseEntityCounter()
    entity
  }

  override def close(): Unit = {
    if(!closed) {
      executionReport.executionDone()
      closed = true
      entities.close()
    }
  }
}

/**
  * An entity iterator that forwards all entity traversals to an execution report.
  */
case class ReportingIterator(entities: CloseableIterator[Entity])
                            (implicit executionReport: ExecutionReportUpdater, prefixes: Prefixes) extends GenericReportingIterator[Entity](entities) {

  private var schemaReported = false

  override protected def addEntitySample(entity: Entity): Unit = {
    if(!schemaReported) {
      schemaReported = true
      executionReport.startNewOutputSamples(entity.schema)
    }
    executionReport.addEntityAsSampleEntity(entity)
  }
}



object ReportingIterator {

  /**
   * Adds an execution reporter to a local entities table and potential sub tables.
   */
  def addReporter(entities: LocalEntities)(implicit executionReport: ExecutionReportUpdater, prefixes: Prefixes): LocalEntities = {
    val reportingTraversable = ReportingIterator(CloseableIterator(entities.entities))(executionReport, prefixes)
    entities match {
      case multiTable: MultiEntityTable =>
        multiTable.copy(entities = reportingTraversable, subTables = multiTable.subTables.map(addReporter))
      case typedEntities: TypedEntities[Any, _] =>
        typedEntities.updateTypedEntities(new GenericReportingIterator[Any](typedEntities.typedEntities))
      case _ =>
        entities.updateEntities(reportingTraversable, entities.entitySchema)
    }
  }

}

