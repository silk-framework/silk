package org.silkframework.plugins.dataset.xml

import org.silkframework.config.Task
import org.silkframework.entity.{EntityTrait, SchemaTrait}
import org.silkframework.execution.local._
import org.silkframework.execution.{ExecutionReport, TaskException}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.resource.InMemoryResourceManager

/**
  * Only considers the first input and checks for the property path defined in the [[XmlParserTask]] specification.
  * It will only read XML field of the first entity.
  */
case class LocalXmlParserTaskExecutor() extends LocalExecutor[XmlParserTask] {
  override def execute(task: Task[XmlParserTask],
                       inputs: Seq[EntityTable[EntityTrait, SchemaTrait]],
                       outputSchemaOpt: Option[SchemaTrait],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)): Option[EntityTable[EntityTrait, SchemaTrait]] = {
    if (inputs.size != 1) {
      throw TaskException("XmlParserTask takes exactly one input!")
    }
    outputSchemaOpt map { os =>
      inputs.head match {
        case entityTable: FlatEntityTable =>
          executeOnFlatEntities(task, os, entityTable)
        case _ =>
          throw TaskException("XML parser task cannot handle nested entities!") // FIXME: Support for nested entities?
      }
    }
  }

  private def executeOnFlatEntities(task: Task[XmlParserTask], os: SchemaTrait, entityTable: FlatEntityTable) = {
    val spec = task.data
    val entities = entityTable.entities
    val pathIndex = spec.parsedInputPath match {
      case Some(path) =>
        entityTable.entitySchema.pathIndex(path)
      case None =>
        0 // Take the value of the first path
    }

    entities.headOption match {
      case Some(entity) =>
        val values = entity.values
        if (values.size <= pathIndex) {
          throw TaskException("No input path with index " + pathIndex + " found!")
        } else {
          values(pathIndex).headOption match {
            case Some(xmlValue) =>
              val resource = InMemoryResourceManager().get("temp")
              resource.write(xmlValue.getBytes)
              val dataset = XmlDataset(resource, spec.basePath, entity.uri + spec.uriSuffixPattern)
              val schema = SchemaTrait.toEntitySchema(os)
              val entities = dataset.source.retrieve(schema)
              GenericEntityTable(entities, schema, task)
            case None =>
              throw TaskException("No value found for input path!")
          }
        }
      case None =>
        throw TaskException("No input entity for LocalXmlParserTaskExecutor found!")
    }
  }
}
