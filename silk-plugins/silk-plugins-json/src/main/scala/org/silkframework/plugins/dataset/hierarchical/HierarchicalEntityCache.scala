package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.dataset.rdf.ClosableIterator
import org.silkframework.runtime.caching.{PersistentSortedKeyValueStore, PersistentSortedKeyValueStoreConfig}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

case class HierarchicalEntityCache() {

  private lazy val cacheId: String = "json-"//TODO generate unique ID after 10GB bug has been fixed + UUID.randomUUID().toString

  private val dir = Files.createTempDirectory("json")

  private val cache = new PersistentSortedKeyValueStore(cacheId, Some(dir.toFile), temporary = true, config = PersistentSortedKeyValueStoreConfig(compressValues = true))

  def putEntity(entity: HierarchicalEntity): Unit = {
    val keyBuffer = cache.createKeyBuffer(entity.uri)

    val bytesOut = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(bytesOut)
    oos.writeObject(entity.values)
    oos.writeInt(entity.tableIndex)
    oos.flush()
    val valueBuffer = cache.createValueBuffer(bytesOut.toByteArray)
    oos.close()

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
    val arrayBuffer = ByteBuffer.allocate(buffer.remaining())
    arrayBuffer.put(buffer)
    val bytesIn = new ByteArrayInputStream(arrayBuffer.array())
    val ois = new ObjectInputStream(bytesIn)
    val values = ois.readObject().asInstanceOf[Seq[Seq[String]]]
    val index = ois.readInt()
    HierarchicalEntity(uri, values, index)
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
