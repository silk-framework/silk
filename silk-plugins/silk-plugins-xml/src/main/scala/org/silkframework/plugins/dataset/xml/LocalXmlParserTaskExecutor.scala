package org.silkframework.plugins.dataset.xml

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, ExecutorRegistry, TaskException}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.runtime.resource.InMemoryResourceManager

import java.nio.charset.StandardCharsets

/**
  * Only considers the first input and checks for the property path defined in the [[XmlParserTask]] specification.
  * It will only read XML field of the first entity.
  */
case class LocalXmlParserTaskExecutor() extends LocalExecutor[XmlParserTask] {
  override def execute(task: Task[XmlParserTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[LocalEntities] = {
    val spec = task.data
    if (inputs.size != 1) {
      throw TaskException("XmlParserTask takes exactly one input!")
    }
    output.requestedSchema flatMap { outputSchema =>
      val entityTable = inputs.head
      val entities = entityTable.entities

      val pathIndex = spec.parsedInputPath match {
        case Some(path) => entityTable.entitySchema.indexOfPath(path)
        case None => 0 // Take the value of the first path
      }

      entities.headOption match {
        case Some(entity) =>
          val values = entity.values
          if(values.size <= pathIndex) {
            throw TaskException("No input path with index " + pathIndex + " found!")
          } else {
            values(pathIndex).headOption match {
              case Some(xmlValue) =>
                val resource = InMemoryResourceManager().get("temp")
                resource.writeBytes(xmlValue.getBytes(StandardCharsets.UTF_8))
                val dataset = XmlDataset(resource, spec.basePath, entity.uri.toString + spec.uriSuffixPattern, streaming = false)
                ExecutorRegistry.execute(PlainTask(task.id, DatasetSpec(dataset, readOnly = true)),
                  Seq.empty, output, LocalExecution(useLocalInternalDatasets = false))
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
