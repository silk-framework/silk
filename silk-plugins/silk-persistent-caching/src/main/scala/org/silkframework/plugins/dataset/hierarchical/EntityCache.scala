package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.runtime.caching.{HandleTooLargeKeyStrategy, PersistentSortedKeyValueStore, PersistentSortedKeyValueStoreConfig}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.StreamUtils
import org.silkframework.util.StreamUtils.ByteBufferBackedInputStream

import java.io._
import java.nio.ByteBuffer
import java.util.UUID

/**
  * Holds entities in a persistent cache.
  */
private case class HierarchicalEntityCache() extends Closeable {

  private lazy val cacheId: String = "entities-" + UUID.randomUUID().toString

  private val cache = new PersistentSortedKeyValueStore(cacheId, None, temporary = true, config = PersistentSortedKeyValueStoreConfig(
    tooLargeKeyStrategy = HandleTooLargeKeyStrategy.TruncateKeyWithHash,
    compressValues = true
  ))

  /**
    * Add an entity to the cache.
    */
  def putEntity(entity: CachedEntity): Unit = {
    val keyBuffer = cache.createKeyBuffer(entity.uri)

    // Write entity into ByteBuffer
    val byteStream = new ByteArrayOutputStream
    val objectStream = new ObjectOutputStream(byteStream)
    try {
      StreamUtils.writeString(objectStream, entity.uri)
      objectStream.writeObject(entity.values)
      objectStream.writeInt(entity.tableIndex)
    } finally {
      objectStream.close()
    }
    val valueBuffer = cache.createValueBuffer(byteStream.toByteArray)

    cache.put(keyBuffer, valueBuffer, None)
  }

  /**
    * Retrieve an entity by URI.
    */
  def getEntity(uri: String): Option[CachedEntity] = {
    for(valueBuffer <- cache.getBytes(uri, None)) yield {
      readEntity(Some(uri), valueBuffer)
    }
  }

  /**
    * Iterate through all entities.
    */
  def iterateEntities: CloseableIterator[CachedEntity] = {
    new EntityIterator(cache.iterateEntries())
  }

  /**
    * Clears this cache and deletes all file resources.
    */
  def close(): Unit = {
    cache.deleteStore()
  }

  /**
    * Reads an entity from a byte buffer.
    */
  private def readEntity(uriOpt: Option[String], buffer: ByteBuffer): CachedEntity = {
    val inputStream = new ObjectInputStream(new ByteBufferBackedInputStream(buffer))
    try {
      val entityUri = StreamUtils.readString(inputStream)
      assert(uriOpt.getOrElse(entityUri) == entityUri, s"Cached entity has a different URI: '$entityUri' vs '$uriOpt'.")
      val values = inputStream.readObject().asInstanceOf[IndexedSeq[Seq[String]]]
      val index = inputStream.readInt()
      CachedEntity(entityUri, values, index)
    } finally {
      inputStream.close()
    }
  }

  private class EntityIterator(iterator: CloseableIterator[(ByteBuffer, ByteBuffer)]) extends CloseableIterator[CachedEntity] {

    override def hasNext: Boolean = iterator.hasNext

    override def next(): CachedEntity = {
      val (_, valueBytes) = iterator.next()
      readEntity(None, valueBytes)
    }

    override def close(): Unit = iterator.close()
  }

}


