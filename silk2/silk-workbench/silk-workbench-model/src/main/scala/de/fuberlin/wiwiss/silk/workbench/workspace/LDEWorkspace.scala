package de.fuberlin.wiwiss.silk.workbench.workspace

import java.net.URI
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workbench.util._
import de.fuberlin.wiwiss.silk.util.Identifier

class LDEWorkspace (workspaceUri : URI) extends Workspace    {

  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  private val prefixes = QueryFactory.getPrefixes

  private val sparqlEndpoint = new RemoteSparqlEndpoint(new URI(workspaceUri+"/sparql"))
  private val sparulEndpoint = new RemoteSparulEndpoint(new URI(workspaceUri+"/sparul"), prefixes)

  private var projectList : List[Project] = {

    val res = sparqlEndpoint.query(QueryFactory.sMappings,100)

    for(projectRes <- res.toList) yield  {
      val projectUri = projectRes("uri").value
      logger.info("Loading Project: "+projectUri)
      new LDEProject(clean(projectUri),sparqlEndpoint,sparulEndpoint)
    }
  }

  override def projects : List[Project] = projectList

  override def createProject(name : Identifier) = {
    logger.info ("Creating new Project: "+name  )
    // TODO check if it already exists..
    sparulEndpoint.query(QueryFactory.iNewProject(name))
    val newProject = new LDEProject(name,sparqlEndpoint,sparulEndpoint)
    projectList ::= newProject
    newProject
  }

  override def removeProject(name : Identifier) =  {
    logger.info ("Deleting Project: "+name  )
    sparulEndpoint.query(QueryFactory.dProject(name))
    projectList = projectList.filterNot(_.name == name)
  }

  def getDatasources : Map[String,String] = {
    val res = sparqlEndpoint.query(QueryFactory.sDataSources)
    var datasources : Map[String,String] = Map.empty
    for(datasource <- res.toList) {
      datasources = datasources + ( datasource("uri").value -> clean(datasource("id").value ) ) 
    }
    datasources
  }

  //def getCategories : Map[String,String] = Map("smwcat:Gene"->"Gene","smwcat:Disease"->"Disease","smwcat:Pathway"->"Pathway")
  def getCategories : Map[String,String] = {
    val res = sparqlEndpoint.query(QueryFactory.sCategories)
    var categories : Map[String,String] = Map.empty
    for(category <- res.toList) {
      categories = categories + ( category("c").value -> clean(category("c").value ) ) 
    }
    categories
  }

  // util
  def clean (uri : String) =  {   uri.split("/").last  }


}

