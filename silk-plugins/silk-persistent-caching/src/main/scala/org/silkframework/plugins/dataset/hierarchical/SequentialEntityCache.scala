package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.util.StreamUtils

import java.io._

/**
  * Persists entities in sequential order.
  */
private case class SequentialEntityCache() {

  private val rootEntityFile = File.createTempFile("sequential-entities", "")

  private val objectStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rootEntityFile)))

  private var isClosed: Boolean = false

  /**
    * Writes an entity.
    */
  def putEntity(uri: String, values: IndexedSeq[Seq[String]]): Unit = {
    objectStream.writeBoolean(true) // signal that another entity is following
    StreamUtils.writeString(objectStream, uri)
    objectStream.writeInt(values.size)
    for(value <- values) {
      objectStream.writeInt(value.size)
      for(v <- value) {
        StreamUtils.writeString(objectStream, v)
      }
    }
  }

  /**
    * Reads all entities and deletes this cache.
    */
  def readAndClose(f: CachedEntity => Unit): Unit = {
    assert(!isClosed, "Cache has been closed.")
    isClosed = true
    objectStream.writeBoolean(false) // signal end of stream
    objectStream.close()

    val inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(rootEntityFile)))
    try {
      while (inputStream.readBoolean()) {
        val uri = StreamUtils.readString(inputStream)
        val values = IndexedSeq.fill(inputStream.readInt())(Seq.fill(inputStream.readInt())(StreamUtils.readString(inputStream)))
        f(CachedEntity(uri, values, 0))
      }
    } finally {
      inputStream.close()
      rootEntityFile.delete()
    }
  }

  def close(): Unit = {
    rootEntityFile.delete()
  }

}
