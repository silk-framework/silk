package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import xml.XML
import java.io.File
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Implementation of a project which is stored on the local file system.
 */
class FileProject(file : File) extends Project
{
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

    def config = SourceConfig()

    def config_=(c : SourceConfig) {}

    def tasks = synchronized
    {
      for(fileName <- file.list.toList) yield
      {
        val source = Source.load(file + ("/" + fileName))

        SourceTask(source)
      }
    }

    def update(task : SourceTask) = synchronized
    {
      task.source.toXML.write(file + ("/" + task.name + ".xml"))
    }

    def remove(taskId : String) = synchronized
    {
      (file + taskId).deleteRecursive()
    }
  }

  /**
   * The linking module which encapsulates all linking tasks.
   */
  class FileLinkingModule(file : File) extends LinkingModule
  {
    def config = LinkingConfig()

    def config_=(c : LinkingConfig) {}

    def tasks = synchronized
    {
      for(fileName <- file.list.toList) yield
      {
        val projectConfig = FileProject.this.config

        val prefixes = Prefixes.fromXML(XML.loadFile(file + ("/" + fileName + "/prefixes.xml")))

        val linkSpec = LinkSpecification.load(prefixes)(file + ("/" + fileName + "/linkSpec.xml"))

        val alignment = AlignmentReader.readAlignment(file + ("/" + fileName + "/alignment.xml"))

        val cache = Cache.fromXML(XML.loadFile(file + ("/" + fileName + "/cache.xml")))

        LinkingTask(fileName, prefixes, linkSpec, alignment, cache)
      }
    }

    def update(task : LinkingTask) = synchronized
    {
      val taskDir = file + ("/" + task.name)
      taskDir.mkdirs()

      task.prefixes.toXML.write(taskDir + "/prefixes.xml")
      task.linkSpec.toXML.write(taskDir+ "/linkSpec.xml")
      task.alignment.toXML.write(taskDir+ "/alignment.xml")
      task.cache.toXML.write(taskDir +  "/cache.xml")
    }

    def remove(taskId : String) = synchronized
    {
      (file + ("/" + taskId)).deleteRecursive()
    }
  }
}