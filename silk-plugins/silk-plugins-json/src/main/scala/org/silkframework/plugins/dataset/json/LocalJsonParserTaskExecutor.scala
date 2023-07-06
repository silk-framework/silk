package org.silkframework.plugins.dataset.json

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.Entity.EntitySerializer
import org.silkframework.entity.{Entity, EntitySchema, MultiEntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, ExecutorRegistry, TaskException}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, RepeatedIterator}
import org.silkframework.runtime.resource.InMemoryResourceManager

import java.io.{DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.file.Files
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
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[LocalEntities] = {
    if (inputs.size != 1) {
      throw TaskException("JsonParserTask takes exactly one input!")
    }
    output.requestedSchema.map { requestedSchema =>
      val entityTable = inputs.head
      val entities = RewindableEntityIterator.load(entityTable.entities, entityTable.entitySchema)

      val pathIndex =  task.data.parsedInputPath match {
        case Some(path) => entityTable.entitySchema.indexOfPath(path)
        case None => 0 // Take the value of the first path
      }

      if(!entities.hasNext) {
        throw TaskException("No input entity for 'JSON Parser Operator' found! There must be at least one entity in the input.")
      }

      val entityParser = new EntityParser(task, output, execution, pathIndex)
      val parsedEntities = new RepeatedIterator(() => entities.nextOption().map(entityParser))

      requestedSchema match {
        case mt: MultiEntitySchema =>
          val subEntities = mt.subSchemata.map { subSchema =>
            val subEntityParser = new EntityParser(task, ExecutorOutput(output.task, Some(subSchema)), execution, pathIndex)
            entities.reset()
            val subParsedEntities = new RepeatedIterator(() => entities.nextOption().map(subEntityParser))
            GenericEntityTable(subParsedEntities, subSchema, task)
          }
          MultiEntityTable(parsedEntities, requestedSchema, task, subEntities)
        case _ =>
          GenericEntityTable(parsedEntities, requestedSchema, task)
      }
    }
  }

  /**
    * Parses individual entities.
    */
  private class EntityParser(task: Task[JsonParserTask], output: ExecutorOutput, execution: LocalExecution, pathIndex: Int)
                            (implicit userContext: UserContext, prefixes: Prefixes) extends ((Entity) => CloseableIterator[Entity]) {

    private val spec = task.data

    /**
      * Takes a single entity and returns the parsed output entities.
      */
    def apply(entity: Entity): CloseableIterator[Entity] = {
      val values = entity.values
      if (values.size <= pathIndex) {
        throw TaskException("Unexpected error: No input value for path with index " + pathIndex + " found for 'JSON Parser Operator'!")
      } else {
        values(pathIndex).headOption match {
          case Some(jsonInputString) =>
            val resource = InMemoryResourceManager().get("temp")
            resource.writeBytes(jsonInputString.getBytes)
            val dataset = JsonDataset(resource, basePath = spec.basePath, uriPattern = entity.uri.toString + spec.uriSuffixPattern, streaming = false)
            ExecutorRegistry.execute(PlainTask(task.id, DatasetSpec(dataset, readOnly = true)), Seq.empty, output, execution) match {
              case Some(result) => result.entities
              case None => CloseableIterator.empty
            }
          case None =>
            throw TaskException("No value found for input path!")
        }
      }
    }
  }

}


trait RewindableIterator[T] extends CloseableIterator[T] {

  /**
    * Resets this iterator to the beginning.
    */
  def reset(): Unit

}


class RewindableEntityIterator(file: File, schema: EntitySchema) extends RewindableIterator[Entity] {

  private var inputStream = new DataInputStream(new FileInputStream(file))

  override def reset(): Unit = {
    inputStream.close()
    inputStream = new DataInputStream(new FileInputStream(file))
  }

  override def hasNext: Boolean = {
    inputStream.available() != 0
  }

  override def next(): Entity = {
    EntitySerializer.deserialize(inputStream, schema)
  }

  override def close(): Unit = {
    try {
      inputStream.close()
    } finally {
      file.delete()
    }
  }
}


object RewindableEntityIterator {

  def load(iterator: CloseableIterator[Entity], schema: EntitySchema): RewindableEntityIterator = {
    val file = Files.createTempFile("rewindableEntityIterator", "tmp").toFile
    file.deleteOnExit()
    val outputStream = new DataOutputStream(new FileOutputStream(file))
    try {
      for (entity <- iterator) {
        EntitySerializer.serialize(entity, outputStream)
      }
    } finally {
      try {
        outputStream.close()
      } finally {
        iterator.close()
      }
    }
    new RewindableEntityIterator(file, schema)
  }

}