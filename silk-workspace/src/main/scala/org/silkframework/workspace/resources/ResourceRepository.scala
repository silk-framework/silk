package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.ResourceManager

trait ResourceRepository {

  def get(project: String): ResourceManager

}
