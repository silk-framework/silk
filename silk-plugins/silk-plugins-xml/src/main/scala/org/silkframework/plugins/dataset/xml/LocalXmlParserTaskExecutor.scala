package org.silkframework.plugins.dataset.xml

import org.silkframework.config.Task
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{ExecutionReport, TaskException}
import org.silkframework.execution.local.{LocalEntities, GenericEntityTable, LocalExecution, LocalExecutor}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.resource.InMemoryResourceManager

/**
  * Only considers the first input and checks for the property path defined in the [[XmlParserTask]] specification.
  * It will only read XML field of the first entity.
  */
case class LocalXmlParserTaskExecutor() extends LocalExecutor[XmlParserTask] {
  override def execute(task: Task[XmlParserTask],
                       inputs: Seq[LocalEntities],
                       outputSchemaOpt: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)): Option[LocalEntities] = {
    val spec = task.data
    if (inputs.size != 1) {
      throw TaskException("XmlParserTask takes exactly one input!")
    }
    outputSchemaOpt map { os =>
      val entityTable = inputs.head
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
          if(values.size <= pathIndex) {
            throw TaskException("No input path with index " + pathIndex + " found!")
          } else {
            values(pathIndex).headOption match {
              case Some(xmlValue) =>
                val resource = InMemoryResourceManager().get("temp", mustExist = false)
                resource.writeBytes(xmlValue.getBytes)
                val dataset = XmlDataset(resource, spec.basePath, entity.uri + spec.uriSuffixPattern, streaming = false)
                val entities = dataset.source.retrieve(os)
                GenericEntityTable(entities, os, Some(task))
              case None =>
                throw TaskException("No value found for input path!")
            }
          }
        case None =>
          throw TaskException("No input entity for LocalXmlParserTaskExecutor found!")
      }
    }
  }
}
