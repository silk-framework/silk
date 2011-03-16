package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import java.util.logging.Logger
import xml.XML
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workbench.util._
import de.fuberlin.wiwiss.silk.config.Prefixes

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
  override def config = {
    if (xmlProj == null)
      ProjectConfig(QueryFactory.getPrefixes)
    else xmlProj.config
  }

   // Writes the updated project configuration.
  override def config_=(config : ProjectConfig)   {
    xmlProj.config = config
    updateTripleStore
  }

   // Merge project and default prefixes
   //  (in case of double id - project prefix has priority)
  def mergePrefixes : Map[String,String] = {
    var prefixesMap = config.prefixes.toMap
    for((key, value) <- QueryFactory.getPrefixes)
      {
        if (!prefixesMap.contains(key))
          prefixesMap += key -> value
      }
    prefixesMap
  }

  // Retrieve project prefixes in Sparql query format
  def projectSparqlPrefixes = {
    config.prefixes.toSparql
  }

   // Update the TripleStore
  private def updateTripleStore = {
    // TODO - use stream - linkSpec.write()
    // delete
    sparulEndpoint.query(QueryFactory.dSourceCode(projectUri))
    // insert updated
    sparulEndpoint.query(QueryFactory.iSourceCode(projectUri,xmlProj.getLinkSpec.toString ))
  }


  /** ----------------------------------------------------------
   *   The source module which encapsulates all data sources.
   *  ---------------------------------------------------------- */
  class LDESourceModule() extends SourceModule
  {                                    
    override def config = SourceConfig()

    override def config_=(c : SourceConfig) {}

    override def tasks = synchronized  {

       // load target datasource  -  Wiki
      val params = Map( "endpointURI" -> sparqlEndpoint.uri.toString,
                        "datasourceUri" -> "http://www.example.org/smw-lde/smwDatasources/Wiki",
                        "id" -> "Wiki")
      var datasources : List[SourceTask] = List(SourceTask(Source("TARGET",DataSource("sparqlEndpoint",params))))

       // load source datasource  - optional
      val res = sparqlEndpoint.query(projectSparqlPrefixes + QueryFactory.sProjectDataSource(projectUri),1)
      if (res.size > 0 )
        {
          val from = res.last("from").value
          logger.info("Loading SOURCE Datasource: "+from)
          datasources ::= loadDatasource(from)
        }

      datasources
    }

    override def update(task : SourceTask) = synchronized {
       // delete datasource
      sparulEndpoint.query(QueryFactory.dDataSource(projectUri))
       // insert datasource link into TS
      val datasourceUri = task.source.dataSource match {case DataSource(_, p) => {p("datasourceUri").toString}}
      sparulEndpoint.query(QueryFactory.iDataSource(projectUri,datasourceUri))
      logger.info("Updated source '"+task.name +"' in project '"+name)
    }

    override def remove(taskId : Identifier) = synchronized {
      // it only allows to remove SOURCE
      if (taskId.equals("SOURCE")){
        // delete datasource link from TS
       sparulEndpoint.query(QueryFactory.dDataSource(projectUri))
       logger.info("Removed source '"+taskId +"' in project '"+name)
      }
    }

    //-  Util functions
    def loadDatasource (dataSourceUri : String) = {

        val res = sparqlEndpoint.query(projectSparqlPrefixes + QueryFactory.sDataSource(dataSourceUri),1)

        if (res.size > 0) {
           val ds = res.last
           val id = ds("id").value
           val endpointUri = sparqlEndpoint.uri.toString
           val params = Map( "endpointURI" -> endpointUri,
                             "id" -> id,
                            //"label" -> label,
                            "datasourceUri" -> dataSourceUri)
           SourceTask(Source("SOURCE",DataSource("sparqlEndpoint",params)))
        }
        else {
           // Datasource definition not found
           // TODO - throw Exception 'Error in retrieving the datasource' ?
           SourceTask(Source("DataSource_Not_Found",DataSource("sparqlEndpoint",Map( "endpointURI" -> ""))))
        }
    }
    
  }



  /** ----------------------------------------------------------
   *   The linking module which encapsulates all linking tasks.
   *  ----------------------------------------------------------- */
  class LDELinkingModule() extends LinkingModule
  {
    @volatile
    private var cachedTasks : Option[Traversable[LinkingTask]] = None

    override def config = LinkingConfig()

    override def config_=(c : LinkingConfig) {}

    override def tasks = synchronized {
      if(cachedTasks.isEmpty)  {
        cachedTasks = Some(loadTasks)
      }
      cachedTasks.get
    }

    override def update(task : LinkingTask) = synchronized  {
      // update XML
      xmlProj.linkingModule.update(task)
      // append default prefixes to the project - in case of new project or linking spec without any prefix defined
      if (xmlProj.getPrefixes.size == 0)   {
        val defaultPrefixes = QueryFactory.getPrefixes
        logger.info ("prefixes.. "+(Prefixes(defaultPrefixes).toXML).toString)
        logger.info ("prefix.. "+(Prefixes(defaultPrefixes).toXML \ "Prefix").toString)
          xmlProj.appendPrefixes(Prefixes(defaultPrefixes).toXML \ "Prefix")
          logger.info("Added prefixes: " + xmlProj.getPrefixes.size)
      }
      // update TS via Sparql\Update
      updateTripleStore
      cachedTasks = None
      logger.info("Updated linking task '"+task.name +"' in project '"+name+"'")
    }

    override def remove(taskId : Identifier) = synchronized {
      // update XML
      xmlProj.linkingModule.remove(taskId)
      // update TS via Sparql\Update
      updateTripleStore
      cachedTasks = None
      logger.info("Removed linking task '"+taskId +"' in project '"+name+"'")
    }
        
    private def loadTasks : Traversable[LinkingTask] = {      
      val res = sparqlEndpoint.query(projectSparqlPrefixes + QueryFactory.sProjectSourceCode(projectUri),1)

      if (res.size > 0 ){

        val sourceCode = res.last("xml").value
        val linkSpec = XML.loadString(sourceCode)
        xmlProj = new XMLProject(linkSpec)
        xmlProj.config = ProjectConfig(mergePrefixes)
        logger.info("Loading LinkingTasks")
      }
      else {
         // if property smw-lde:sourceCode is not defined - create an empty project
        xmlProj = new XMLProject(<Silk />)
        logger.warning("The TripleStore doesn't contain a proper Silk Link Specification for resource '"+projectUri+"'")
      }

      val taskSeq = xmlProj.linkingModule.tasks
      // XMLProject doesn't have datasouce info - since those are not in the sourceCode
      for (task <- taskSeq ){
         task.loadCache(LDEProject.this)
        }

      taskSeq
    }

  }

}