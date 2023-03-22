package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.ResourceManager

/**
  * A resource repository that uses the same resource manager for all projects.
  */
case class ConstantResourceRepository(resourceManager: ResourceManager) extends ResourceRepository with SharedResourceRepository
