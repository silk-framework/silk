package org.silkframework.plugins.dataset.hierarchical

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.silkframework.dataset.rdf.ClosableIterator
import org.silkframework.runtime.caching.{PersistentSortedKeyValueStore, PersistentSortedKeyValueStoreConfig}

import java.io.{ByteArrayOutputStream, Closeable, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

/**
  * Holds entities in a persistent cache.
  */
case class EntityCache() extends Closeable {

  private lazy val cacheId: String = "entities-" + UUID.randomUUID().toString

  //private val dir = Files.createTempDirectory("entities")

  private val cache = new PersistentSortedKeyValueStore(cacheId, None, temporary = true, config = PersistentSortedKeyValueStoreConfig(compressValues = true))

  /**
    * Add an entity to the cache.
    */
  def putEntity(entity: CachedEntity): Unit = {
    val keyBuffer = cache.createKeyBuffer(entity.uri)

    // Write entity into ByteBuffer
    val byteStream = new ByteArrayOutputStream
    val objectStream = new ObjectOutputStream(byteStream)
    try {
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
      readEntity(uri, valueBuffer)
    }
  }

  /**
    * Iterate through all entities.
    */
  def iterateEntities: ClosableIterator[CachedEntity] = {
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
  private def readEntity(uri: String, buffer: ByteBuffer): CachedEntity = {
    val inputStream = new ObjectInputStream(new ByteBufferBackedInputStream(buffer))
    try {
      val values = inputStream.readObject().asInstanceOf[Seq[Seq[String]]]
      val index = inputStream.readInt()
      CachedEntity(uri, values, index)
    } finally {
      inputStream.close()
    }
  }

  private class EntityIterator(iterator: ClosableIterator[(ByteBuffer, ByteBuffer)]) extends ClosableIterator[CachedEntity] {

    override def hasNext: Boolean = iterator.hasNext

    override def next(): CachedEntity = {
      val (keyBytes, valueBytes) = iterator.next()
      val uri = UTF_8.decode(keyBytes).toString
      readEntity(uri, valueBytes)
    }

    override def close(): Unit = iterator.close()
  }

}

/**
  * A cached entity.
  *
  * @param uri The URI of the entity
  * @param values The values of the entity
  * @param tableIndex The index of the table this entity belongs to.
  */
case class CachedEntity(uri: String, values: Seq[Seq[String]], tableIndex: Int)
