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
