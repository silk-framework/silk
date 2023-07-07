package org.silkframework.plugins.dataset.json

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.Entity.EntitySerializer
import org.silkframework.entity.{Entity, EntitySchema, MultiEntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, ExecutorRegistry, TaskException}
import org.silkframework.plugins.dataset.json.FileRewindableEntityIterator.TempFileHolder
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
      val entities = FileRewindableEntityIterator.load(entityTable.entities, entityTable.entitySchema)

      val pathIndex =  task.data.parsedInputPath match {
        case Some(path) => entityTable.entitySchema.indexOfPath(path)
        case None => 0 // Take the value of the first path
      }

      if(!entities.hasNext) {
        throw TaskException("No input entity for 'JSON Parser Operator' found! There must be at least one entity in the input.")
      }

      def parseEntities(schema: EntitySchema): CloseableIterator[Entity] = {
        val entityParser = new EntityParser(task, ExecutorOutput(output.task, Some(schema)), execution, pathIndex)
        val entityIterator = entities.newIterator()
        new RepeatedIterator(() => entityIterator.nextOption().map(entityParser))
      }

      requestedSchema match {
        case mt: MultiEntitySchema =>
          val rootEntities = parseEntities(requestedSchema)
          val subEntities = mt.subSchemata.map { subSchema =>
            GenericEntityTable(parseEntities(subSchema), subSchema, task)
          }
          MultiEntityTable(rootEntities, requestedSchema, task, subEntities)
        case _ =>
          GenericEntityTable(parseEntities(requestedSchema), requestedSchema, task)
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


/**
  * Iterator that can be reset to the beginning, i.e., can be iterated multiple times.
  */
trait RewindableIterator[T] extends CloseableIterator[T] {

  /**
    * Returns a new iterator that starts at the beginning.
    */
  def newIterator(): CloseableIterator[T]

}

/**
  * Entity iterator that can be reset to the beginning, i.e., can be iterated multiple times.
  */
trait RewindableEntityIterator extends RewindableIterator[Entity]

object RewindableEntityIterator {

  /**
    * Creates a rewindable entity iterator, possibly by loading all entities into a temporary file.
    */
  def load(iterator: CloseableIterator[Entity], schema: EntitySchema): RewindableEntityIterator = {
    if (iterator.isInstanceOf[RewindableEntityIterator]) {
      // Iterator is already rewindable
      iterator.asInstanceOf[RewindableEntityIterator]
    } else if (!iterator.hasNext) {
      // Iterator is empty
      new InMemoryRewindableEntityIterator(Iterable.empty)
    } else {
      val firstEntity = iterator.next()
      if(!iterator.hasNext) {
        // Iterator contains just one entity
        new InMemoryRewindableEntityIterator(Iterable(firstEntity))
      } else {
        // Load entities into file
        FileRewindableEntityIterator.load(iterator, schema)
      }
    }
  }
}

/**
  * Entity iterator that holds all entities in memory and thus can be rewinded.
  */
class InMemoryRewindableEntityIterator(entities: Iterable[Entity]) extends RewindableEntityIterator {

  private val entityIterator = entities.iterator

  /**
    * Returns a new iterator that starts at the beginning.
    */
  override def newIterator(): CloseableIterator[Entity] = {
    new InMemoryRewindableEntityIterator(entities)
  }

  override def hasNext: Boolean = {
    entityIterator.hasNext
  }

  override def next(): Entity = {
    entityIterator.next()
  }

  override def close(): Unit = {
  }
}

/**
  * Entity iterator that holds all entities on the file system and thus can be rewinded.
  */
class FileRewindableEntityIterator(file: TempFileHolder, schema: EntitySchema) extends RewindableEntityIterator {

  private val inputStream = new DataInputStream(new FileInputStream(file.newInstance()))

  private var nextAvailable = inputStream.readBoolean()

  override def newIterator(): RewindableIterator[Entity] = {
    new FileRewindableEntityIterator(file, schema)
  }

  override def hasNext: Boolean = {
    nextAvailable
  }

  override def next(): Entity = {
    val  entity = EntitySerializer.deserialize(inputStream, schema)
    nextAvailable = inputStream.readBoolean()
    entity
  }

  override def close(): Unit = {
    try {
      inputStream.close()
    } finally {
      file.removeInstance()
    }
  }
}


object FileRewindableEntityIterator {

  def load(iterator: CloseableIterator[Entity], schema: EntitySchema): FileRewindableEntityIterator = {
    val file = new TempFileHolder("RewindableEntityIterator")
    val outputStream = new DataOutputStream(new FileOutputStream(file()))
    try {
      for (entity <- iterator) {
        outputStream.writeBoolean(true)
        EntitySerializer.serialize(entity, outputStream)
      }
      outputStream.writeBoolean(false)
    } finally {
      try {
        outputStream.close()
      } finally {
        iterator.close()
      }
    }
    new FileRewindableEntityIterator(file, schema)
  }

  /**
    * Holds a temporary file that can be used by multiple instances.
    * Deletes the file if the last instance has been removed.
    */
  class TempFileHolder(name: String) {

    private val file = Files.createTempFile(name, "tmp").toFile
    file.deleteOnExit()

    private var instanceCount = 0

    def apply(): File = {
      file
    }

    def newInstance(): File = {
      instanceCount += 1
      file
    }

    def removeInstance(): Unit = {
      instanceCount -= 1
      if(instanceCount <= 0) {
        file.delete()
      }
    }

  }

}