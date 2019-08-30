package org.silkframework.plugins.dataset.json

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, ExecutorRegistry, TaskException}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
/**
  * Only considers the first input and checks for the property path defined in the [[JsonParserTask]] specification.
  * It will only read a property value of the first entity, following entities are ignored.
  */
case class LocalJsonParserTaskExecutor() extends LocalExecutor[JsonParserTask] {
  override def execute(task: Task[JsonParserTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
                      (implicit userContext: UserContext): Option[LocalEntities] = {
    val spec = task.data
    if (inputs.size != 1) {
      throw TaskException("JsonParserTask takes exactly one input!")
    }
    output.requestedSchema.flatMap { _ =>
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
            throw TaskException("Unexpected error: No input value for path with index " + pathIndex + " found for 'JSON Parser Operator'!")
          } else {
            values(pathIndex).headOption match {
              case Some(jsonInputString) =>
                val resource = InMemoryResourceManager().get("temp")
                resource.writeBytes(jsonInputString.getBytes)
                val dataset = JsonDataset(resource, spec.basePath, entity.uri.toString + spec.uriSuffixPattern)
                ExecutorRegistry.execute(PlainTask(task.id, DatasetSpec(dataset)),
                  Seq.empty, output, execution)
              case None =>
                throw TaskException("No value found for input path!")
            }
          }
        case None =>
          throw TaskException("No input entity for 'JSON Parser Operator' found! There must be at least one entity in the input.")
      }
    }
  }
}
