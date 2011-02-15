package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import java.util.logging.Logger
import xml.{Elem, XML}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint

/**
 * Implementation of a project which is stored on the MediaWiki LDE TripleStore - OntoBroker.
 */
class LDEProject(projectUri : String, storeEndpoint : RemoteSparqlEndpoint) extends Project
{

  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  logger.info("Creating new Project: "+projectUri)

  // - import xml matching desc as input stream
  var query = "SELECT ?xml ?from ?to FROM smwGraphs:MappingRepository WHERE  { <"+projectUri+"> smw-lde:sourceCode ?xml . <"+projectUri+"> smw-lde:linksFrom ?from .  <"+projectUri+">	smw-lde:linksTo ?to }"
  val proj = storeEndpoint.query(query,1).last
  val linkSpec = XML.loadString(proj("xml").value)

  // Create XML sub project
  val xmlProj = new XMLProject(linkSpec)

  /**
   * The name of this project
   */
  override val name = new Identifier(cleanPath(projectUri))

  /**
   * The source module which encapsulates all data sources.
   */
  override val sourceModule = new LDESourceModule(linkSpec)

    /**
   * The linking module which encapsulates all linking tasks.
   */
  override val linkingModule = new LDELinkingModule(linkSpec)

  // retrieve datasource tasks
  val from = proj("from").value
  logger.info("Adding Datasource: "+from)
  sourceModule.update(createDatasource(from, "SOURCE"))

  val to = proj("to").value
  logger.info("Adding Datasource: "+to)
  sourceModule.update(createDatasource(to, "TARGET"))

  // retrieve linking tasks
  logger.info("Adding LinkingTasks... (from XML) ")
  linkingModule.tasks
  logger.info("Project: "+ name.toString+" has "+sourceModule.tasks.size.toString+" Sources and "+ linkingModule.tasks.size.toString+ " Link defined")


//-  Util functions
  def createDatasource (storeUri : String, id : String) = {
        query = "SELECT ?url ?label FROM smwGraphs:DataSourceInformationGraph WHERE   { <"+storeUri+"> smw-lde:sparqlEndpointLocation ?url . <"+storeUri+"> smw-lde:label ?label  }"
        val dsl = storeEndpoint.query(query,1)

        if (dsl.size > 0) {
            val ds = dsl.last
            // TODO - label is null in TS
            val label = ds("label").value
            val url = ds("url").value
            SourceTask(Source(id,DataSource("sparqlEndpoint",Map("endpointURI" -> url, "storeURI" -> storeUri, "label" -> label))))
        }
        else if (id.equals("TARGET")){
            // TODO - any def for WIKI dataSource..
            SourceTask(Source(id,DataSource("sparqlEndpoint",Map("endpointURI" -> "http://www.example.org/nullvalue", "storeURI" -> null, "label" -> "WIKI"))))
        }
        else {
            // TODO Throw an exception?
            SourceTask(Source("toChange",DataSource("sparqlEndpoint",Map("endpointURI" -> "http://www.example.org/nullvalue", "storeURI" -> null, "label" -> null))))
        }
  }

  def cleanPath (uri : String) =
  {
    uri.split("/").last
  }

  /**
   * Reads the project configuration.
   */
  override def config =
  {
    ProjectConfig()
  }

  /**
   * Writes the updated project configuration.
   */
  override def config_=(config : ProjectConfig)
  {
  }

  /**
   * The source module which encapsulates all data sources.
   */
  class LDESourceModule(linkSpec : Elem) extends SourceModule
  {                                    
    def config = SourceConfig()

    def config_=(c : SourceConfig) {}

    def tasks = synchronized
    {
      xmlProj.sourceModule.tasks
    }

    def update(task : SourceTask) = synchronized
    {
      xmlProj.sourceModule.update(task)
      // TODO - update TS using a proper REST call
      logger.info("Updated source '"+task.name +"' in project '"+name)
    }

    def remove(taskId : Identifier) = synchronized
    {
      xmlProj.sourceModule.remove(taskId)
      // TODO - update TS using a proper REST call
      logger.info("Removed source '"+taskId +"' in project '"+name)
    }
  }

  /**
   * The linking module which encapsulates all linking tasks.
   */
  class LDELinkingModule(linkSpec : Elem) extends LinkingModule
  {
    def config = LinkingConfig()

    def config_=(c : LinkingConfig) {}

    def tasks = synchronized
    {
       xmlProj.linkingModule.tasks
    }

    def update(task : LinkingTask) = synchronized
    {

      xmlProj.linkingModule.update(task)
      // TODO - update TS using a proper REST call
      logger.info("Updated linking task '"+task.name +"' in project '"+name)
    }

    def remove(taskId : Identifier) = synchronized
    {
      xmlProj.linkingModule.remove(taskId)
      // TODO - update TS using a proper REST call
      logger.info("Removed linking task '"+taskId +"' in project '"+name)
    }
  }

}