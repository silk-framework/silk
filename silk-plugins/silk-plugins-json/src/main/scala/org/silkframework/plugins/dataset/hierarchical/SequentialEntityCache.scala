package org.silkframework.plugins.dataset.hierarchical

import java.io._

/**
  * Persists entities in sequential order.
  */
private case class SequentialEntityCache() {

  private val rootEntityFile = File.createTempFile("sequential-entities", "")

  private val objectStream = new ObjectOutputStream(new FileOutputStream(rootEntityFile))

  private var isClosed: Boolean = false

  /**
    * Writes an entity.
    */
  def putEntity(uri: String, values: Seq[Seq[String]]): Unit = {
    objectStream.writeBoolean(true) // signal that another entity is following
    objectStream.writeUTF(uri)
    objectStream.writeObject(values)
  }

  /**
    * Reads all entities and deletes this cache.
    */
  def readAndClose(f: CachedEntity => Unit): Unit = {
    assert(!isClosed, "Cache has been closed.")
    isClosed = true
    objectStream.writeBoolean(false) // signal end of stream
    objectStream.close()

    val inputStream = new ObjectInputStream(new FileInputStream(rootEntityFile))
    try {
      while (inputStream.readBoolean()) {
        val uri = inputStream.readUTF()
        val values = inputStream.readObject().asInstanceOf[Seq[Seq[String]]]
        f(CachedEntity(uri, values, 0))
      }
    } finally {
      inputStream.close()
      rootEntityFile.delete()
    }
  }

}
