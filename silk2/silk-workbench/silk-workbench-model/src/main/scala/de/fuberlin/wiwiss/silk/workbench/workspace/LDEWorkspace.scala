package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workbench.util._


class LDEWorkspace (workspaceUri : URI) extends Workspace    {

  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  val prefixes = Prefixes (QueryFactory.getPrefixes)

  val sparqlEndpoint = new RemoteSparqlEndpoint(new URI(workspaceUri+"/sparql"), prefixes)
  val sparulEndpoint = new RemoteSparulEndpoint(new URI(workspaceUri+"/sparul"), prefixes)

  val res = sparqlEndpoint.query(QueryFactory.sMappings,100)

  // Create a workspace as collection of LDEProjects
  override def projects = for(projectRes <- res) yield  {
    new LDEProject(projectRes("uri").value,sparqlEndpoint,sparulEndpoint)
  }

  override def createProject(name : String) = {
    logger.info ("Creating new Project: "+name  )
    // TODO check if it already exists..
    sparulEndpoint.query(QueryFactory.iNewProject(name))
  }

  override def removeProject(name : String) =  {
    logger.info ("Deleting Project: "+name  )
    sparulEndpoint.query(QueryFactory.dProject(name))
  }
}
