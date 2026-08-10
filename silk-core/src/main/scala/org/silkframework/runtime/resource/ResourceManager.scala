package org.silkframework.runtime.resource

/**
 * Reads and writes resources.
 */
trait ResourceManager extends ResourceLoader with ResourceWriter with AutoCloseable {

  /**
    * Gets a child resource manager
    */
  override def child(name: String): ResourceManager

  /**
    * Gets the parent resource manager.
    */
  override def parent: Option[ResourceManager]

  /**
    * Gets a resource by its path relative to this resource manager.
    *
    * @param path A path of the form child1/child2/file.ext
    */
  def getInPath(path: String, mustExist: Boolean = false): WritableResource = {
    var loader: ResourceManager = this
    val segments = path.split("/")
    for(segment <- segments.dropRight(1)) {
      loader = loader.child(segment)
    }
    loader.get(segments.last, mustExist)
  }

  override def close(): Unit = {
    // close all child managers
    listChildren.foreach(child => this.child(child).close())
  }
}

object ResourceManager {

  /**
    * Checks that a resource name stays inside the resource manager it is resolved against.
    * Implementations that resolve a name into a real path, or into a key that may be normalised on the way to the
    * storage backend, must call this before resolving. [[FileResourceManager]] does not, because it compares canonical
    * paths, which is a stronger check.
    *
    * @throws ResourceAccessDeniedException If the name would address a location outside of the base path.
    */
  def checkName(name: String, basePath: String): Unit = {
    def deny(reason: String): Nothing = {
      throw ResourceAccessDeniedException(s"Illegal resource name '$name' below '$basePath': $reason.")
    }
    if(name.startsWith("/") || name.startsWith("\\")) {
      deny("absolute names are not permitted")
    }
    if(name.contains("://")) {
      deny("names must not address another storage location")
    }
    if(name.split("[/\\\\]").contains("..")) {
      deny("names must not point outside of the resource base path")
    }
  }
}
