package de.fuberlin.wiwiss.silk.workbench.workspace


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
  def project(name : String) : Project =
  {
    projects.find(_.name == name).getOrElse(throw new NoSuchElementException("Project '" + name + "' not found"))
  }

  def createProject(name : String)

  def removeProject(name : String)
}
