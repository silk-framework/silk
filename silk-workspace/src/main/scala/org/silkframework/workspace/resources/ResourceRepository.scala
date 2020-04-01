package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

/**
  * Holds resources for projects.
  */
trait ResourceRepository {

  /**
    * If true, all projects share the same resources.
    */
  def sharedResources: Boolean

  /**
    * Retrieves all resources for a given project.
    */
  def get(project: Identifier): ResourceManager

  /**
    * Removes project resources that are not shared between projects.
    * Does not delete any resources for repositories that share resources between projects.
    */
  def removeProjectResources(project: Identifier): Unit

}

/**
  * Resource repository for which each project manages its own resources.
  */
trait PerProjectResourceRepository { self: ResourceRepository =>

  /**
    * Base resource manager.
    */
  protected def resourceManager: ResourceManager

  /**
    * Returns false, because projects do not share resources.
    */
  def sharedResources: Boolean = false

  /**
    * Retrieves all resources for a given project.
    */
  def get(project: Identifier): ResourceManager = {
    resourceManager.child(project).child("resources")
  }

  /**
    * Removes project resources.
    */
  override def removeProjectResources(project: Identifier): Unit = {
    resourceManager.delete(project)
  }

}

/**
  * Resource repository for which all projects share the same resources.
  */
trait SharedResourceRespository { self: ResourceRepository =>

  /**
    * Base resource manager.
    */
  protected def resourceManager: ResourceManager

  /**
    * Returns true, because projects share resources.
    */
  def sharedResources: Boolean = false

  /**
    * Retrieves all resources.
    */
  def get(project: Identifier): ResourceManager = {
    resourceManager
  }

  /**
    * Does not delete any resources, since resources are shared.
    */
  override def removeProjectResources(project: Identifier): Unit = { }

}