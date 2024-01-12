package org.silkframework.execution.local

import java.util.logging.{Level, Logger}

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.entity.ValueType
import org.silkframework.execution.EntityHolder
import org.silkframework.runtime.activity.UserContext

/**
  * Writes all entities with failures to a given error sink.
  * The error descriptions are written to an extra column.
  */
object ErrorOutputWriter {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * The property that holds the error descriptions.
    */
  val errorProperty: TypedProperty = TypedProperty("error", ValueType.STRING, isBackwardProperty = false)

  /**
    * Writes all entities with failures to a given error sink.
    */
  def write(entities: EntityHolder, errorSink: EntitySink)
           (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    entities match {
      case tables: MultiEntityTable =>
        for(table <- tables.allTables) {
          writeErrorEntities(errorSink, table)
        }
      case _ =>
        writeErrorEntities(errorSink, entities)
    }
  }

  private def writeErrorEntities(sink: EntitySink, entityTable: EntityHolder)
                                (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    sink.openTable(entityTable.entitySchema.typeUri, entityTable.entitySchema.typedPaths.map(_.property.get) :+ errorProperty, singleEntity = false)
    entityTable.use { entities =>
      for (entity <- entities if entity.hasFailed) {
        sink.writeEntity(entity.uri, entity.values :+ Seq(entity.failure.get.message.getOrElse("Unknown error")))
        entityCount += 1
        if (entityCount % 10000 == 0) {
          val currentTime = System.currentTimeMillis()
          if (currentTime - 2000 > lastLog) {
            log.info("Writing error entities: " + entityCount)
            lastLog = currentTime
          }
        }
      }
    }
    sink.closeTable()
    val time = (System.currentTimeMillis - startTime) / 1000.0
    log.log(Level.INFO, "Finished writing " + entityCount + " error entities with type '" + entityTable.entitySchema.typeUri + "' in " + time + " seconds")
  }

}
