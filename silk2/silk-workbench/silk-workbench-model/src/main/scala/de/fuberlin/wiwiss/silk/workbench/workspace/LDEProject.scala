package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import java.util.logging.Logger
import xml.{Elem, XML}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workbench.util._

/**
 * Implementation of a project which is stored on the MediaWiki LDE TripleStore - OntoBroker.
 */
class LDEProject(projectName : String, sparqlEndpoint : RemoteSparqlEndpoint, sparulEndpoint : RemoteSparulEndpoint) extends Project
{
  private val logger = Logger.getLogger(classOf[LDEProject].getName)

   // The name of this project
  override val name = new Identifier(projectName)

  val projectUri = QueryFactory.dataSourceLinks+projectName

   // The source module which encapsulates all data sources.
  override val sourceModule = new LDESourceModule()

   // The linking module which encapsulates all linking tasks.
  override val linkingModule = new LDELinkingModule()

   // The XML sub project
  var xmlProj : XMLProject = null

  // Reads the project configuration.
  override def config =  {    ProjectConfig()  }

   // Writes the updated project configuration.
  override def config_=(config : ProjectConfig)   { }
  

  /** ----------------------------------------------------------
   *   The source module which encapsulates all data sources.
   *  ---------------------------------------------------------- */
  class LDESourceModule() extends SourceModule
  {                                    
    override def config = SourceConfig()

    override def config_=(c : SourceConfig) {}

    override def tasks = synchronized  {

       // load target datasource  -  Wiki
      logger.info("Loading TARGET Datasource: Wiki")
      val params = Map( "endpointURI" -> sparqlEndpoint.uri.toString,
                       // TODO - "graph" -> "http://www.example.org/nullvalue",
                       "tripleStoreUri" -> "http://www.example.org/smw-lde/smwDatasources/Wiki",
                       "label" -> "Wiki" )
      var datasources : List[SourceTask] = List(SourceTask(Source("TARGET",DataSource("sparqlEndpoint",params))))

       // load source datasource  - optional
      val res = sparqlEndpoint.query(QueryFactory.sProjectDataSource(projectUri),1)
      if (res.size > 0 )
        {
          val from = res.last("from").value
          logger.info("Loading SOURCE Datasource: "+from)
          datasources ::= loadDatasource(from)
        }

      datasources
    }

    override def update(task : SourceTask) = synchronized {
      // TODO - working in progress
       // delete
      //sparulEndpoint.query(QueryFactory.dDataSource(projectUri))
       // insert datasource link into TS
       // val dataSourceName = ((task.source.toXML \ "param").filter(n => (n \ "@name").text.equals("label")).first \ "@value").text
      sparulEndpoint.query(QueryFactory.iDataSource(projectUri,task.name))
      logger.info("Updated source '"+task.name +"' in project '"+name)
    }

    override def remove(taskId : Identifier) = synchronized {
      if (taskId.equals("SOURCE")){
        // delete datasource link from TS
       sparulEndpoint.query(QueryFactory.dDataSource(projectUri))
       logger.info("Removed source '"+taskId +"' in project '"+name)
      }
    }

    //-  Util functions
    def loadDatasource (dataSourceUri : String) = {

        val res = sparqlEndpoint.query(QueryFactory.sDataSource(dataSourceUri),1)

        if (res.size > 0) {
           val ds = res.last
           val label = ds("label").value
            // 'url' contains the remote datasource SPARQL endpoint - but identity resolution works on local data, therefore TripleStore SPARQL endpoint is used as datasource endpoint
            //  val url = ds("url").value
           val url = sparqlEndpoint.uri.toString
           val graph =  ds("graph").value
           val params = Map( "endpointURI" -> url,
                            "label" -> label,
                            "graph" -> graph,
                            "tripleStoreURI" -> dataSourceUri )
           SourceTask(Source("SOURCE",DataSource("sparqlEndpoint",params)))
        }
        else {
           // Datasource definition not found
           // TODO - throw Exception 'Error in retrieving the datasource' ?
           SourceTask(Source("Not_Found",DataSource("sparqlEndpoint",Map())))
        }
    }
    
  }



  /** ----------------------------------------------------------
   *   The linking module which encapsulates all linking tasks.
   *  ----------------------------------------------------------- */
  class LDELinkingModule() extends LinkingModule
  {
    override def config = LinkingConfig()

    override def config_=(c : LinkingConfig) {}

    override def tasks = synchronized {

      val res = sparqlEndpoint.query(QueryFactory.sProjectSourceCode(projectUri),1)

      if (res.size > 0 ){
      
        val sourceCode = res.last("xml").value
        val linkSpec = XML.loadString(sourceCode)
        xmlProj = new XMLProject(linkSpec)
        logger.info("Loading LinkingTasks")
      }
      else {
         // if property smw-lde:sourceCode is not defined - create an empty project
        xmlProj = new XMLProject(<Silk />)
        logger.warning("The TripleStore doesn't contain a proper Silk Link Specification for resource '"+projectUri+"'")
      }

      xmlProj.linkingModule.tasks
    }

    override def update(task : LinkingTask) = synchronized  {
      // update XML
      xmlProj.linkingModule.update(task)
      // update TS via Sparql\Update
      updateTripleStore
      logger.info("Updated linking task '"+task.name +"' in project '"+name+"'")
    }

    override def remove(taskId : Identifier) = synchronized {
      // update XML
      xmlProj.linkingModule.remove(taskId)
      // update TS via Sparql\Update
      updateTripleStore
      logger.info("Removed linking task '"+taskId +"' in project '"+name+"'")
    }

    private def updateTripleStore = {
      // TODO - use stream - linkSpec.write()
      // delete
      sparulEndpoint.query(QueryFactory.dSourceCode(projectUri))
      // insert updated
      sparulEndpoint.query(QueryFactory.iSourceCode(projectUri,xmlProj.getLinkSpec.toString ))
    }
  }

}