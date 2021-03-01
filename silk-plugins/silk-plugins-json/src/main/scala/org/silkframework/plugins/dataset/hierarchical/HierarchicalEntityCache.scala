package org.silkframework.plugins.dataset.hierarchical

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.silkframework.dataset.rdf.ClosableIterator
import org.silkframework.runtime.caching.{PersistentSortedKeyValueStore, PersistentSortedKeyValueStoreConfig}

import java.io.{ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

case class HierarchicalEntityCache() {

  private lazy val cacheId: String = "json-"//TODO generate unique ID after 10GB bug has been fixed + UUID.randomUUID().toString

  private val dir = Files.createTempDirectory("json")

  private val cache = new PersistentSortedKeyValueStore(cacheId, Some(dir.toFile), temporary = true, config = PersistentSortedKeyValueStoreConfig(compressValues = true))

  def putEntity(entity: HierarchicalEntity): Unit = {
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

  def getEntity(uri: String): Option[HierarchicalEntity] = {
    for(valueBuffer <- cache.getBytes(uri, None)) yield {
      readEntity(uri, valueBuffer)
    }
  }

  def iterateEntities: ClosableIterator[HierarchicalEntity] = {
    new EntityIterator(cache.iterateEntries())
  }

  private def readEntity(uri: String, buffer: ByteBuffer): HierarchicalEntity = {
    val inputStream = new ObjectInputStream(new ByteBufferBackedInputStream(buffer))
    try {
      val values = inputStream.readObject().asInstanceOf[Seq[Seq[String]]]
      val index = inputStream.readInt()
      HierarchicalEntity(uri, values, index)
    } finally {
      inputStream.close()
    }
  }

  private class EntityIterator(iterator: ClosableIterator[(ByteBuffer, ByteBuffer)]) extends ClosableIterator[HierarchicalEntity] {

    override def hasNext: Boolean = iterator.hasNext

    override def next(): HierarchicalEntity = {
      val (keyBytes, valueBytes) = iterator.next()
      val uri = UTF_8.decode(keyBytes).toString
      readEntity(uri, valueBytes)
    }

    override def close(): Unit = iterator.close()
  }

}

case class HierarchicalEntity(uri: String, values: Seq[Seq[String]], tableIndex: Int)
