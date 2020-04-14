package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

/**
  * Holds resources for projects.
  */
trait ResourceRepository {

  /**
    * Retrieves all resources for a given project.
    */
  def get(project: Identifier): ResourceManager

}
