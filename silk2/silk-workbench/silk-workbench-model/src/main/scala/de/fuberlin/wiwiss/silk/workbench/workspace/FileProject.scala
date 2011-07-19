package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{Cache, LinkingTask, LinkingConfig, LinkingModule}
import modules.output.{OutputTask, OutputConfig, OutputModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import xml.XML
import java.io.File
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.config.Prefixes
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.util.{Timer, Identifier}
import de.fuberlin.wiwiss.silk.output.Output

/**
 * Implementation of a project which is stored on the local file system.
 */
class FileProject(file : File) extends Project
{
  private implicit val logger = Logger.getLogger(classOf[FileProject].getName)

  private var cachedConfig : Option[ProjectConfig] = None

  private var changed = false

  /**
   * The name of this project
   */
  override val name : Identifier = file.getName

  /**
   * Reads the project configuration.
   */
  override def config =
  {
    if(cachedConfig.isEmpty)
    {
      val configFile = file + "/config.xml"

      if(configFile.exists)
      {
        val configXML = XML.loadFile(configFile)
        val prefixes = Prefixes.fromXML(configXML \ "Prefixes" head)
        cachedConfig = Some(ProjectConfig(prefixes))
      }
      else
      {
        cachedConfig = Some(ProjectConfig.default)
      }
    }

    cachedConfig.get
  }

  /**
   * Writes the updated project configuration.
   */
  override def config_=(config : ProjectConfig)
  {
    val configXMl =
      <ProjectConfig>
      { config.prefixes.toXML }
      </ProjectConfig>

    configXMl.write(file + "/config.xml")

    cachedConfig = Some(config)
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
   * The output module which encapsulates all output tasks.
   */
  override val outputModule = new FileOutputModule(file + "/output")

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
    @volatile
    private var cachedTasks : Map[Identifier, LinkingTask] = load()

    @volatile
    private var updatedTasks = Map[Identifier, LinkingTask]()

    @volatile
    private var lastUpdateTime = 0L

    WriteThread.start()

    override def config = LinkingConfig()

    override def config_=(c : LinkingConfig) {}

    override def tasks =
    {
      cachedTasks.values
    }

    override def update(task : LinkingTask)
    {
      cachedTasks += (task.name -> task)
      updatedTasks += (task.name -> task)
      lastUpdateTime = System.currentTimeMillis

      logger.info("Updated linking task '" + task.name + "' in project '" + name + "'")
    }

    override def remove(taskId : Identifier)
    {
      (file + ("/" + taskId)).deleteRecursive()

      cachedTasks -= taskId
      updatedTasks -= taskId

      logger.info("Removed linking task '" + taskId + "' from project '" + name + "'")
    }

    private def load() : Map[Identifier, LinkingTask] =
    {
      file.mkdir()

      val tasks =
        for(fileName <- file.list.toList) yield
        {
          val projectConfig = FileProject.this.config

          val linkSpec = LinkSpecification.load(projectConfig.prefixes)(file + ("/" + fileName + "/linkSpec.xml"))

          val alignment = AlignmentReader.readAlignment(file + ("/" + fileName + "/alignment.xml"))

          val cache =
            try
            {
              Cache.fromXML(XML.loadFile(file + ("/" + fileName + "/cache.xml")), FileProject.this, linkSpec, alignment)
            }
            catch
            {
              case ex : Exception =>
              {
                logger.warning("Cache corrupted. Rebuilding Cache.")
                val cache = new Cache()
                cache.reload(FileProject.this, linkSpec, alignment)
                cache
              }
            }

          LinkingTask(linkSpec, alignment, cache)
        }

      tasks.map(task => (task.name, task)).toMap
    }

    def write()
    {
      val tasksToWrite = updatedTasks.values.toList
      updatedTasks --= tasksToWrite.map(_.name)

      for(task <- tasksToWrite) Timer("Writing task " + task.name + " to disk")
      {
        val taskDir = file + ("/" + task.name)
        taskDir.mkdir()

        //Don't use any prefixes
        implicit val prefixes = Prefixes.empty

        task.linkSpec.toXML.write(taskDir+ "/linkSpec.xml")
        task.alignment.toXML.write(taskDir+ "/alignment.xml")
        task.cache.toXML.write(taskDir +  "/cache.xml")
      }
    }

    object WriteThread extends Thread
    {
      private val interval = 5000L

      override def run()
      {
        while(true)
        {
          val time = System.currentTimeMillis - lastUpdateTime

          if(updatedTasks.isEmpty)
          {
            Thread.sleep(interval)
          }
          else if(time >= interval)
          {
            try
            {
              linkingModule.write()
            }
            catch
            {
              case ex : Exception => logger.log(Level.WARNING, "Error writing linking tasks", ex)
            }
          }
          else
          {
            Thread.sleep(interval - time)
          }
        }
      }
    }
  }
  
   /**
   * The linking module which encapsulates all linking tasks.
   */
  class FileOutputModule(file : File) extends OutputModule
  {
    file.mkdirs()

    def config = OutputConfig()

    def config_=(c: OutputConfig) { }

    override def tasks = synchronized
    {
      for(fileName <- file.list.toList) yield
      {
        val output = Output.load(file + ("/" + fileName))

        OutputTask(output)
      }
    }

    override def update(task : OutputTask) = synchronized
    {
      task.output.toXML.write(file + ("/" + task.name + ".xml"))
      logger.info("Updated output '" + task.name + "' in project '" + name + "'")
    }

    override def remove(taskId : Identifier) = synchronized
    {
      (file + ("/" + taskId + ".xml")).deleteRecursive()
      logger.info("Removed output '" + taskId + "' from project '" + name + "'")
    }
  }
}