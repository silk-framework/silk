package org.silkframework.runtime.resource

import java.io._
import java.time.Instant

/**
  * A resource manager which holds all data in memory
  */
case class InMemoryResourceManager() extends InMemoryResourceManagerBase()

/**
  * Base class of [[InMemoryResourceManager]] for avoiding leaking implementation details.
  *
  * Folders are only listed once they have been written to, see `materialized`.
  */
class InMemoryResourceManagerBase(val basePath: String = "",
                                  parentMgr: Option[InMemoryResourceManagerBase] = None,
                                  folderName: String = "") extends ResourceManager {

  import InMemoryResourceManagerBase.Entry

  // Both maps may be updated concurrently, e.g., task XML and cache files share the same folder.
  // All mutations must be synchronized on this instance; reads are lock-free via @volatile.
  // Locks are only ever nested child before parent (see materialize), never parent before child.

  /** Holds all resources at this path. */
  @volatile private var resources = Map[String, Entry]()

  /** Holds all child resource managers that have been accessed, including ones nothing has been written to yet. */
  @volatile private var children = Map[String, InMemoryResourceManagerBase]()

  /** True once a resource has been written into this folder or one of its descendants. Only materialized children are listed. */
  @volatile private var materialized = false

  /**
    * Retrieves a name resource.
    *
    * @param name      The name of the resource.
    * @param mustExist If true, an ResourceNotFoundException is thrown if the resource does not exist
    * @return The resource.
    * @throws ResourceNotFoundException If no resource with the given name has been found.
    */
  override def get(name: String, mustExist: Boolean): WritableResource = {
    val path = basePath + "/" + name

    resources.get(name) match {
      case Some(_) => new InMemoryWritableResource(name, path)
      case None if !mustExist => new InMemoryWritableResource(name, path)
      case None => throw new ResourceNotFoundException(s"Resource $name not found in path $basePath")
    }
  }

  var label = "no name"

  /**
    * Lists all available resources.
    */
  override def list: List[String] = resources.keys.toList

  override def listChildren: List[String] = children.collect { case (name, child) if child.materialized => name }.toList

  override def child(name: String): ResourceManager = synchronized {
    children.get(name) match {
      case Some(childMgr) => childMgr
      case None =>
        val childMgr = new InMemoryResourceManagerBase(basePath + "/" + name, Some(this), name)
        children += ((name, childMgr))
        childMgr
    }
  }

  override def parent: Option[ResourceManager] = parentMgr

  override def delete(name: String): Unit = {
    for(childToDelete <- children.get(name)) {
      for(childFolders <- childToDelete.listChildren) {
        childToDelete.delete(childFolders)
      }
      for(childResources <- childToDelete.list) {
        childToDelete.get(childResources).delete()
      }
    }
    synchronized {
      resources -= name
      children -= name
    }
  }

  /** Stores a resource. Like writing a file on disk, this also creates the folders. Must be called while holding this instance's monitor. */
  private def store(name: String, bytes: Array[Byte], append: Boolean): Unit = {
    val allBytes =
      resources.get(name) match {
        case Some(entry) if append =>
          entry.data ++ bytes
        case _ =>
          bytes
      }
    resources += ((name, Entry(allBytes, Instant.now())))
    materialize()
  }

  /**
    * Marks this folder and all of its ancestors as written to, re-attaching a deleted folder like a file write recreates
    * directories. Must be called while holding this instance's monitor; ancestors are updated under their own monitor.
    */
  private def materialize(): Unit = {
    materialized = true
    for(parent <- parentMgr) {
      parent.synchronized {
        parent.children.get(folderName) match {
          case None =>
            parent.children += ((folderName, this))
            parent.materialize()
          case Some(current) if current eq this =>
            parent.materialize()
          case Some(_) =>
            // Superseded by a folder of the same name, even one only looked up since the delete: the stale write is lost
        }
      }
    }
  }

  /**
    * A resource that is held in memory.
    */
  private class InMemoryWritableResource(val name: String, val path: String) extends WritableResource {

    override def exists: Boolean = resources.contains(name)

    override def size: Option[Long] = resources.get(name).map(_.data.length.toLong)

    override def modificationTime: Option[Instant] = resources.get(name).map(_.modificationTime)

    override def inputStream: InputStream = {
      resources.get(name) match {
        case Some(entry) => new ByteArrayInputStream(entry.data)
        case None => new ByteArrayInputStream(Array.empty)
      }
    }

    /**
      * Creates an output stream for writing to this resource.
      * The caller is responsible for closing the stream after writing.
      * Using [[write()]] is preferred as it takes care of closing the output stream.
      */
    override def createOutputStream(append: Boolean = false): OutputStream = {
      new InMemoryOutputStream(append)
    }

    /**
      * Writes raw bytes.
      * Overridden for performance.
      */
    override def writeBytes(bytes: Array[Byte], append: Boolean = false): Unit = InMemoryResourceManagerBase.this.synchronized {
      store(name, bytes, append)
    }

    // Deletes the resource only; a child folder of the same name stays (on disk a name is either a file or a directory).
    override def delete(): Unit = InMemoryResourceManagerBase.this.synchronized {
      resources -= name
    }

    private class InMemoryOutputStream(append: Boolean) extends OutputStream {
      private val outputStream = new ByteArrayOutputStream()
      private var closed = false

      override def write(b: Int): Unit = { ensureOpen(); outputStream.write(b) }
      override def write(b: Array[Byte]): Unit = { ensureOpen(); outputStream.write(b) }
      override def write(b: Array[Byte], off: Int, len: Int): Unit = { ensureOpen(); outputStream.write(b, off, len) }
      override def flush(): Unit = outputStream.flush()

      // Writing to a closed stream fails instead of silently dropping the data.
      private def ensureOpen(): Unit = if(closed) throw new IOException("Stream Closed")

      // Idempotent like FileOutputStream.close: a second close must not apply the buffer again.
      override def close(): Unit = InMemoryResourceManagerBase.this.synchronized {
        if(!closed) {
          closed = true
          store(name, outputStream.toByteArray, append)
        }
      }
    }

  }

}

object InMemoryResourceManagerBase {

  /** The data of a resource and the time of its last write. */
  private final case class Entry(data: Array[Byte], modificationTime: Instant)

}
