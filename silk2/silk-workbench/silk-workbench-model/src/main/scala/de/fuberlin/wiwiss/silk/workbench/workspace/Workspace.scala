package de.fuberlin.wiwiss.silk.workbench.workspace


trait Workspace
{
   /**
   *  Retrieves the projects in this workspace
   */
  def projects : Traversable[Project]
  
}