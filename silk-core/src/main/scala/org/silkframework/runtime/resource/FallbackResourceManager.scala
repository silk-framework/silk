package org.silkframework.runtime.resource

import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
  * A ResourceManager that consists of a primary resource manager and a fallback resource loader.
  * If a requested resource is not available in the primary resource manager it is retrieved from the fallback resource loader.
  * Write operations are always executed on the primary resource manager, which creates a resource if necessary.
  * Navigation operations (child, parent) are only applied on the primary resource manager, the fallback resource loader always stays at the same path.
  * The FallbackResourceManager can be used for adding default resources which are always available.
  */
case class FallbackResourceManager(resourceMgr: ResourceManager, fallbackLoader: ResourceManager, writeIntoFallbackLoader: Boolean, basePath: String = "") extends ResourceManager {

  override def get(name: String, mustExist: Boolean): WritableResource = {
    if(mustExist) {
      try {
        resourceMgr.get(name, mustExist = true)
      } catch {
        case _: ResourceNotFoundException =>
          ReadOnlyResource(fallbackLoader.get(name, mustExist = true))
      }
    } else {
      FallBackResource(resourceMgr.get(name, mustExist = false), fallbackLoader.get(name, mustExist = false), basePath + "/" + name)
    }
  }

  override def child(name: String): ResourceManager = {
    // fallback loader also needs to return the child
    FallbackResourceManager(resourceMgr.child(name), fallbackLoader, writeIntoFallbackLoader, basePath + "/" + name)
  }

  override def parent: Option[ResourceManager] = {
    for(parent <- resourceMgr.parent) yield {
      FallbackResourceManager(parent, fallbackLoader, writeIntoFallbackLoader, parent.basePath) // TODO: fallback loader also needs to return the parent
    }
  }

  override def delete(name: String): Unit = resourceMgr.delete(name)

  override def list: List[String] = resourceMgr.list ++ fallbackLoader.list

  override def listChildren: List[String] = resourceMgr.listChildren

  case class FallBackResource(primaryResource: WritableResource, fallbackResource: WritableResource, path: String) extends WritableResource {
    /**
      * The local name of this resource.
      */
    override def name: String = primaryResource.name

    /**
      * Checks if this resource exists.
      */
    override def exists: Boolean = primaryResource.exists || fallbackResource.exists

    override def size: Option[Long] = {
      if (primaryResource.exists) {
        primaryResource.size
      } else if (fallbackResource.exists) {
        fallbackResource.size
      } else {
        None
      }
    }

    override def modificationTime: Option[Instant] = {
      if (primaryResource.exists) {
        primaryResource.modificationTime
      } else if (fallbackResource.exists) {
        fallbackResource.modificationTime
      } else {
        None
      }
    }

    /**
      * Creates an output stream for writing to this resource.
      * The caller is responsible for closing the stream after writing.
      * Using [[write()]] is preferred as it takes care of closing the output stream.
      */
    def createOutputStream(append: Boolean = false): OutputStream = {
      if(writeIntoFallbackLoader && !primaryResource.exists && fallbackResource.exists) {
        fallbackResource.createOutputStream(append)
      } else {
        primaryResource.createOutputStream(append)
      }
    }

    /**
      * Loads the resource.
      *
      * @return An input stream for reading the resource.
      *         The caller is responsible for closing the stream after reading.
      */
    override def inputStream: InputStream = {
      if(primaryResource.exists) {
        primaryResource.inputStream
      } else {
        fallbackResource.inputStream
      }
    }

    /**
      * Deletes this resource.
      */
    override def delete(): Unit = primaryResource.delete()
  }

}

