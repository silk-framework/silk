package de.fuberlin.wiwiss.silk.runtime.resource

import java.io._

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
    * @param name The name of the resource.
    * @param mustExist If true, an ResourceNotFoundException is thrown if the resource does not exist
    * @return The resource.
    * @throws ResourceNotFoundException If no resource with the given name has been found.
    */
  override def get(name: String, mustExist: Boolean): WritableResource = {
    val path = basePath + name
    resources.get(name) match {
      case Some(data) => new InMemoryResource(name, path, data)
      case None if !mustExist => new InMemoryResource(name, path, Array[Byte]())
      case None if mustExist => throw new ResourceNotFoundException(s"Resource $name not found in path $basePath")
    }
  }

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
        children += ((name,  childMgr))
        childMgr
    }
  }

  override def parent: Option[ResourceManager] = parentMgr

  override def delete(name: String): Unit = {
    resources -= name
  }

  /**
    * A resource that is held in memory.
    */
  private class InMemoryResource(val name: String, val path: String, var data: Array[Byte]) extends WritableResource {

    override def load: InputStream = new ByteArrayInputStream(data)

    override def write(write: (OutputStream) => Unit): Unit = {
      val outputStream = new ByteArrayOutputStream()
      write(outputStream)
      data = outputStream.toByteArray
    }
  }
}
