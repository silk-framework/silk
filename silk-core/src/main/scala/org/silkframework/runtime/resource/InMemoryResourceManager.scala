package org.silkframework.runtime.resource

import java.io._
import java.time.Instant

/**
  * A resource manager which holds all data in memory
  */
case class InMemoryResourceManager() extends InMemoryResourceManagerBase()

/**
  * Base class of [[InMemoryResourceManager]] for avoiding leaking implementation details.
  */
class InMemoryResourceManagerBase(val basePath: String = "", parentMgr: Option[InMemoryResourceManagerBase] = None) extends ResourceManager {

  /** Holds all resources at this path. */
  @volatile private var resources = Map[String, Array[Byte]]()

  /** Hold all non-empty child resource managers. */
  @volatile private var children = Map[String, InMemoryResourceManagerBase]()

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
      case Some(data) => new InMemoryWritableResource(name, path)
      case None if !mustExist => new InMemoryWritableResource(name, path)
      case None => throw new ResourceNotFoundException(s"Resource $name not found in path $basePath")
    }
  }

  var label = "no name"

  /**
    * Lists all available resources.
    */
  override def list: List[String] = resources.keys.toList

  override def listChildren: List[String] = children.keys.toList

  override def child(name: String): ResourceManager = {
    children.get(name) match {
      case Some(childMgr) => childMgr
      case None =>
        val childMgr = new InMemoryResourceManagerBase(basePath + "/" + name, Some(this))
        children += ((name, childMgr))
        childMgr
    }
  }

  override def parent: Option[ResourceManager] = parentMgr

  override def delete(name: String): Unit = {
    val childToDelete = child(name)
    for(childFolders <- childToDelete.listChildren) {
      childToDelete.delete(childFolders)
    }
    for(childResources <- childToDelete.list) {
      childToDelete.get(childResources).delete()
    }
    resources -= name
    children -= name
  }

  /**
    * A resource that is held in memory.
    */
  private class InMemoryWritableResource(val name: String, val path: String) extends WritableResource {

    override def exists: Boolean = resources.contains(name)

    override def size: Option[Long] = {
      resources.get(name) match {
        case Some(data) => Some(data.length.toLong)
        case None => None
      }
    }

    override def modificationTime: Option[Instant] = None

    override def inputStream: InputStream = {
      resources.get(name) match {
        case Some(data) => new ByteArrayInputStream(data)
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
    override def writeBytes(bytes: Array[Byte], append: Boolean = false): Unit = {
      val allBytes =
        resources.get(name) match {
          case Some(data) if append =>
            data ++ bytes
          case _ =>
            bytes
        }

      resources += ((name, allBytes))
    }

    override def delete(): Unit = {
      resources -= name
      children -= name
    }

    private class InMemoryOutputStream(append: Boolean) extends OutputStream {
      private val outputStream = new ByteArrayOutputStream()

      override def write(b: Int): Unit = outputStream.write(b)
      override def write(b: Array[Byte]): Unit = outputStream.write(b)
      override def write(b: Array[Byte], off: Int, len: Int): Unit = outputStream.write(b, off, len)
      override def flush(): Unit = outputStream.flush()
      override def close(): Unit = {
        val bytes =
          resources.get(name) match {
            case Some(data) if append =>
              data ++ outputStream.toByteArray
            case _ =>
              outputStream.toByteArray
          }

        resources += ((name, bytes))
      }
    }

  }

}