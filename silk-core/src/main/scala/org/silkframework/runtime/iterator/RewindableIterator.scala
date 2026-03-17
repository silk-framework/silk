package org.silkframework.runtime.iterator

import org.silkframework.entity.Entity.EntitySerializer
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.iterator.FileRewindableEntityIterator.TempFileHolder

import java.io.{DataInputStream, DataOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.file.Files

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
        FileRewindableEntityIterator.load(CloseableIterator(Iterator(firstEntity) ++ iterator, iterator), schema)
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

/**
 * Entity iterator that may re-create the iterator using a creation fuction and thus can be rewinded.
 *
 * Implementing classes need to override the newIterator() method.
 */
abstract class FunctionRewindableEntityIterator extends RewindableEntityIterator {

  @volatile
  private var initialEntityIterator: Option[CloseableIterator[Entity]] = None

  override final def hasNext: Boolean = {
    createInitialEntityIterator().hasNext
  }

  override final def next(): Entity = {
    createInitialEntityIterator().next()
  }

  override final def close(): Unit = {
    initialEntityIterator.foreach(_.close())
    initialEntityIterator = None
  }

  private def createInitialEntityIterator(): CloseableIterator[Entity] = {
    initialEntityIterator match {
      case Some(iterator) =>
        iterator
      case None =>
        initialEntityIterator = Some(newIterator())
        initialEntityIterator.get
    }
  }
}
