package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.sparql.{Node, RemoteSparqlEndpoint}


class LDEWorkspace (workspaceUri : URI) extends Workspace
{

  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  val prefixes = Prefixes (Map ("smwGraphs" -> "http://www.example.org/smw-lde/smwGraphs/", "smwDatasourceLinks" -> "http://www.example.org/smw-lde/smwDatasourceLinks/", "smw-lde" -> "http://www.example.org/smw-lde/smw-lde.owl#", "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#"  ))
  
  val storeEndpoint = new RemoteSparqlEndpoint(workspaceUri, prefixes)

  val query = " SELECT  ?uri FROM smwGraphs:MappingRepository  WHERE  { ?uri rdf:type smw-lde:SilkMatchingDescription }"

  val res = storeEndpoint.query(query,100)



  // Create a workspace as collection of LDEProjects
  override def projects = for(projectRes <- res) yield
      {
        new LDEProject(projectRes("uri").value,storeEndpoint)
      }

  override def createProject(name : String) =
  {
    //TODO
  }

  override def removeProject(name : String) =
  {
    //TODO
  }
}
