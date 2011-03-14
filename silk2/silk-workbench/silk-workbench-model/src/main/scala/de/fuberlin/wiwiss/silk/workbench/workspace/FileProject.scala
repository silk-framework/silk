package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{Cache, LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import xml.XML
import java.io.File
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.config.Prefixes
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * Implementation of a project which is stored on the local file system.
 */
class FileProject(file : File) extends Project
{
  private val logger = Logger.getLogger(classOf[FileProject].getName)

  /**
   * The name of this project
   */
  override val name : Identifier = file.getName

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
  override val sourceModule = new FileSourceModule(file + "/source")

  /**
   * The linking module which encapsulates all linking tasks.
   */
  override val linkingModule = new FileLinkingModule(file + "/linking")

  /**
   * The source module which encapsulates all data sources.
   */
  class FileSourceModule(file : File) extends SourceModule
  {
    file.mkdirs()

    override def config = SourceConfig()

    override def config_=(c : SourceConfig) {}

    override def tasks = synchronized
    {
      for(fileName <- file.list.toList) yield
      {
        val source = Source.load(file + ("/" + fileName))

        SourceTask(source)
      }
    }

    override def update(task : SourceTask) = synchronized
    {
      task.source.toXML.write(file + ("/" + task.name + ".xml"))
      logger.info("Updated source '" + task.name + "' in project '" + name + "'")
    }

    override def remove(taskId : Identifier) = synchronized
    {
      (file + ("/" + taskId + ".xml")).deleteRecursive()
      logger.info("Removed source '" + taskId + "' from project '" + name + "'")
    }
  }

  /**
   * The linking module which encapsulates all linking tasks.
   */
  class FileLinkingModule(file : File) extends LinkingModule
  {
    file.mkdir()

    @volatile
    private var cachedTasks : Option[Traversable[LinkingTask]] = None

    override def config = LinkingConfig()

    override def config_=(c : LinkingConfig) {}

    override def tasks = synchronized
    {
      if(cachedTasks.isEmpty)
      {
        cachedTasks = Some(loadTasks)
      }

      cachedTasks.get
    }

    override def update(task : LinkingTask) = synchronized
    {
      val taskDir = file + ("/" + task.name)
      taskDir.mkdir()

      implicit val prefixes = task.prefixes

      task.prefixes.toXML.write(taskDir + "/prefixes.xml")
      task.linkSpec.toXML.write(taskDir+ "/linkSpec.xml")
      task.alignment.toXML.write(taskDir+ "/alignment.xml")
      task.cache.toXML.write(taskDir +  "/cache.xml")

      task.loadCache(FileProject.this)

      cachedTasks = None

      logger.info("Updated linking task '" + task.name + "' in project '" + name + "'")
    }

    override def remove(taskId : Identifier) = synchronized
    {
      (file + ("/" + taskId)).deleteRecursive()

      cachedTasks = None

      logger.info("Removed linking task '" + taskId + "' from project '" + name + "'")
    }

    private def loadTasks : Traversable[LinkingTask] =
    {
      for(fileName <- file.list.toList) yield
      {
        val projectConfig = FileProject.this.config

        val prefixes = Prefixes.fromXML(XML.loadFile(file + ("/" + fileName + "/prefixes.xml")))

        val linkSpec = LinkSpecification.load(prefixes)(file + ("/" + fileName + "/linkSpec.xml"))

        val alignment = AlignmentReader.readAlignment(file + ("/" + fileName + "/alignment.xml"))

        val cache = Cache.fromXML(XML.loadFile(file + ("/" + fileName + "/cache.xml")))

        val linkingTask = LinkingTask(fileName, prefixes, linkSpec, alignment, cache)

        linkingTask.loadCache(FileProject.this)

        linkingTask
      }
    }
  }
}