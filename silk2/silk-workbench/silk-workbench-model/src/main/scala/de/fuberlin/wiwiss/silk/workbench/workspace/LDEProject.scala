package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import java.util.logging.Logger
import xml.{Elem, XML}


/**
 * Implementation of a project which is stored on the MediaWiki LDE TripleStore - OntoBroker.
 */
class LDEProject(projectUri : String) extends Project
{

  private val logger = Logger.getLogger(classOf[LDEProject].getName)

  // TODO - retrieve xml link specification from the TS, using the proper REST call
  // - get sparql/rest endpoint client
  // - import xml matching desc as input stream

  // TODO - remove
  val xmlStream = getClass().getClassLoader().getResourceAsStream(projectUri)
  val linkSpec = XML.load(xmlStream)

  val xmlProj = new XMLProject(linkSpec)

  // TODO - remove - only for testing
  private def writeLiskSpec(){
    val out = new java.io.FileWriter("linkSpecTmp.xml")
    out.write( xmlProj.getLinkSpec.toString)
    out.close
  }

  /**
   * The name of this project
   */
  override val name = xmlProj.name

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
  override val sourceModule = new LDESourceModule(linkSpec)

  /**
   * The linking module which encapsulates all linking tasks.
   */
  override val linkingModule = new LDELinkingModule(linkSpec)

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
      // TODO - update TS using a proper REST call - is that supported?
      writeLiskSpec

      logger.info("[LDEProject: "+projectUri +"] [LinkingTask: "+task.name +"] UPDATE")
    }

    def remove(taskId : String) = synchronized
    {
      xmlProj.sourceModule.remove(taskId)
      // TODO - update TS using a proper REST call - is that supported?
      writeLiskSpec

      logger.info("[LDEProject: "+projectUri +"] [LinkingTask: "+taskId +"] REMOVE")
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
      writeLiskSpec

      logger.info("[LDEProject: "+projectUri +"] [LinkingTask: "+task.name +"] UPDATE")
    }

    def remove(taskId : String) = synchronized
    {
      xmlProj.linkingModule.remove(taskId)
      // TODO - update TS using a proper REST call
      writeLiskSpec

      logger.info("[LDEProject: "+projectUri +"] [LinkingTask: "+taskId +"] REMOVE")
    }
  }

}