package de.fuberlin.wiwiss.silk.workbench.workspace

import de.fuberlin.wiwiss.silk.util.Identifier


trait Workspace
{
  /**
   * Retrieves the projects in this workspace
   */
  def projects : Traversable[Project]

  /**
   * Retrieves a project by name.
   *
   * @throws java.util.NoSuchElementException If no project with the given name has been found
   */
  def project(name : Identifier) : Project =
  {
    projects.find(_.name == name).getOrElse(throw new NoSuchElementException("Project '" + name + "' not found"))
  }

  def createProject(name : Identifier) : Project

  def removeProject(name : Identifier)
}
