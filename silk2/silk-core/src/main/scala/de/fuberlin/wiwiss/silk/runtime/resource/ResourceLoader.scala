package de.fuberlin.wiwiss.silk.runtime.resource

/**
 * Loads external resources that are required by a data set, such as files.
 */
trait ResourceLoader {

  /**
   * Lists all available resources.
   */
  def list: List[String]

  /**
   * Retrieves a name resource.
   *
   * @param name The name of the resource.
   * @return The resource.
   * @throws ResourceNotFoundException If no resource with the given name has been found.
   */
  def get(name: String): Resource

  def listChildren: List[String]

  def child(name: String): ResourceLoader
}
