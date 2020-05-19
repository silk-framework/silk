package org.silkframework.runtime.resource

/**
 * Writes resources.
 */
trait ResourceWriter {

  /**
    * Retrieves a named resource whose data can be written.
    *
    * @param name The name of the resource.
    * @param mustExist If true, an ResourceNotFoundException is thrown if the resource does not exist
    * @return The resource.
    * @throws ResourceNotFoundException If no resource with the given name has been found and mustExist is set to true.
    */
  def get(name: String, mustExist: Boolean = false): WritableResource

  /**
    * Deletes a resource or child resource manager by name.
    *
    * @param name The name of the resource or child resource manager.
    */
  def delete(name: String)
}
