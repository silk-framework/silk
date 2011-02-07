package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI


class LDEWorkspace (workspaceUri : URI) extends Workspace
{

  // TODO - Get REST endpoint
  val restEndpoint = Nil // = new RestClient(URI)      

  // TODO - Retrieve projects from REST endpoint
  val result : List[String] = restEndpoint //.getProjectList()

  // Create a workspace as collection of LDEProjects
  override def projects : List[Project] = for(projectUri <- result) yield
      {
        new LDEProject(projectUri)
      }

}