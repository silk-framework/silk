/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import modules.output.{MemoryOutputModule, OutputConfig, OutputTask, OutputModule}
import java.util.logging.Logger
import xml.XML
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.workspace.util._
import de.fuberlin.wiwiss.silk.config.Prefixes
import collection.mutable.SynchronizedQueue
import de.fuberlin.wiwiss.silk.util.plugin.EmptyResourceManager

/**
 * Implementation of a project which is stored on the MediaWiki LDE TripleStore - OntoBroker.
 */
class LDEProject(projectName : String, sparqlEndpoint : RemoteSparqlEndpoint, sparulEndpoint : RemoteSparulEndpoint) extends Project
{
  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  override val resourceManager = new EmptyResourceManager

   // The name of this project
  override val name = new Identifier(projectName)

  val projectUri = QueryFactory.dataSourceLinks+projectName

   // The XML sub project
  var xmlProj : XMLProject = null

   // The source module which encapsulates all data sources.
  override val sourceModule = new LDESourceModule()

   // The linking module which encapsulates all linking tasks.
  override val linkingModule = new LDELinkingModule()

  override val outputModule = new MemoryOutputModule()

  // Reads the project configuration.
  override def config = {
    if (xmlProj==null)
      ProjectConfig(QueryFactory.getLDEDefaultPrefixes)
    else {
      if (xmlProj.getPrefixes.size==0){
        val defaultPrefixes = Prefixes(QueryFactory.getLDEDefaultPrefixes).toXML \ "Prefix"
        xmlProj.appendPrefixes(defaultPrefixes)
      }
      xmlProj.config
    }
  }

   // Writes the updated project configuration.
  override def config_=(config : ProjectConfig)   {
    xmlProj.config = config
    updateTripleStore
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

      var targetDatasourceUri :String = null
      var datasources : List[SourceTask] = List.empty

       // load source datasource  - optional
      val res = sparqlEndpoint.query(QueryFactory.sProjectDataSource(projectUri),1)
      if (res.size > 0 )
        {
          targetDatasourceUri = res.last("from").value
          logger.info("Loading SOURCE Datasource: "+targetDatasourceUri)
          datasources ::= loadDatasource(targetDatasourceUri)
        }

       // load target datasource  -  Wiki
      val params = Map( "endpointURI" -> sparqlEndpoint.uri.toString,
                        "datasourceUri" -> "http://www.example.org/smw-lde/smwDatasources/Wiki",
                        "excludedDatasourceUri" -> targetDatasourceUri,
                        "id" -> "Wiki")
      datasources ::= SourceTask(LDEProject.this, Source("TARGET",DataSource("LDEsparqlEndpoint",params)))

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

        val res = sparqlEndpoint.query(QueryFactory.sDataSource(dataSourceUri),1)

        if (res.size > 0) {
           val ds = res.last
           val id = ds("id").value
           val endpointUri = sparqlEndpoint.uri.toString
           val params = Map( "endpointURI" -> endpointUri,
                             "id" -> id,
                            "datasourceUri" -> dataSourceUri)
           SourceTask(LDEProject.this, Source("SOURCE", DataSource("LDEsparqlEndpoint",params)))
        }
        else {
           // Datasource definition not found
           // TODO - throw Exception 'Error in retrieving the datasource' ?
           SourceTask(LDEProject.this, Source("DataSource_Not_Found", DataSource("LDEsparqlEndpoint",Map( "endpointURI" -> ""))))
        }
    }
  }



  /** ----------------------------------------------------------
   *   The linking module which encapsulates all linking tasks.
   *  ----------------------------------------------------------- */
  class LDELinkingModule() extends LinkingModule
  {
    @volatile
    private var cachedTasks : Map[Identifier, LinkingTask] = load()

    @volatile
    private var updatedTasks = Map[Identifier, LinkingTask]()

    @volatile
    private var lastUpdateTime = 0L

    override def config = LinkingConfig()

    override def config_=(c : LinkingConfig) {}

    override def tasks =
    {
      cachedTasks.values
    }

    override def update(task : LinkingTask) {
      // update XML
      xmlProj.linkingModule.update(task)

      // update TS via Sparql\Update
      updateTripleStore

      cachedTasks += (task.name -> task)
      updatedTasks += (task.name -> task)
      lastUpdateTime = System.currentTimeMillis

      logger.info("Updated linking task '"+task.name +"' in project '"+name+"'")
    }

    override def remove(taskId : Identifier) {
      // update XML
      xmlProj.linkingModule.remove(taskId)
      // update TS via Sparql\Update
      updateTripleStore

      cachedTasks -= taskId
      updatedTasks -= taskId

      logger.info("Removed linking task '"+taskId +"' in project '"+name+"'")
    }

    private def load() : Map[Identifier, LinkingTask] = {
      val res = sparqlEndpoint.query(QueryFactory.sProjectSourceCode(projectUri),1)

      if (res.size > 0 ){

        val sourceCode = res.last("xml").value
        val linkSpec = XML.loadString(sourceCode)
        xmlProj = new XMLProject(linkSpec)
        logger.info("Loading LinkingTasks")
      }
      else {
         // if property smw-lde:sourceCode is not defined or create a new empty project
        xmlProj = new XMLProject(<Silk />)
        logger.warning("The TripleStore doesn't contain a proper Silk Link Specification for resource '"+projectUri+"'")
      }

      val taskSeq = xmlProj.linkingModule.tasks
      // XMLProject doesn't have datasouce info - since those are not in the sourceCode

      taskSeq.map(task => (task.name, task)).toMap
    }

  }

}