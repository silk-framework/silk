package org.silkframework.runtime.resource

/**
  * Loads external resources that are required by a data set, such as files.
  */
trait ResourceLoader {

  /**
    * The full path of this resource loader.
    */
  def basePath: String

  /**
    * Lists all available resources. Only direct child resources are
    * returned.
    */
  def list: List[String]

  /**
    * Lists all available resources in the base path.
    * All resources in all subdirectories of the base path will be returned.
    */
  def listRecursive: List[String] = {
    val children = for (child <- this.listChildren) yield this.child(child).listRecursive
    this.list ++ children.flatten
  }

  /**
    * Retrieves a named resource.
    *
    * @param name      The name of the resource.
    * @param mustExist If true, an ResourceNotFoundException is thrown if the resource does not exist
    * @return The resource.
    * @throws ResourceNotFoundException If no resource with the given name has been found and mustExist is set to true.
    */
  def get(name: String, mustExist: Boolean = false): Resource

  def listChildren: List[String]

  def child(name: String): ResourceLoader

  def parent: Option[ResourceLoader]
}
