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
    val configFile = file + "/config.xml"
    if(configFile.exists)
    {
      val configXML = XML.loadFile(file + "/config.xml")

      val prefixes = (configXML \ "Prefixes" \ "Prefix").map(n => (n \ "@id" text, n \ "@namespace" text)).toMap

      new ProjectConfig(prefixes)
    }
    else
    {
      new ProjectConfig(Map.empty)
    }
  }

  /**
   * Writes the updated project configuration.
   */
  override def config_=(config : ProjectConfig)
  {
    <Project>
      <Prefixes>
      {
        for((key, value) <- config.prefixes) yield
        {
          <Prefix id={key} namespace={value} />
        }
      }
      </Prefixes>
    </Project>
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

    def tasks =
    {
      for(fileName <- file.list.toList) yield
      {
        val source = Source.load(file + ("/" + fileName))

        SourceTask(source)
      }
    }

    def update(task : SourceTask)
    {
      task.source.toXML.write(file + ("/" + task.name + ".xml"))
    }

    def remove(task : SourceTask)
    {
      (file + task.name).deleteRecursive()
    }
  }

  /**
   * The linking module which encapsulates all linking tasks.
   */
  class FileLinkingModule(file : File) extends LinkingModule
  {
    file.mkdirs()

    def config = LinkingConfig()

    def config_=(c : LinkingConfig) {}

    def tasks =
    {
      for(fileName <- file.list.toList) yield
      {
        val projectConfig = FileProject.this.config

        val linkSpec = LinkSpecification.load(projectConfig.prefixes)(file + ("/" + fileName + "/linkSpec.xml"))

        val alignment = AlignmentReader.readAlignment(file + ("/" + fileName + "/alignment.xml"))

        val cache = Cache.fromXML(XML.loadFile(file + ("/" + fileName + "/cache.xml")))

        LinkingTask(fileName, linkSpec, alignment, cache)
      }
    }

    def update(task : LinkingTask)
    {
      task.linkSpec.toXML.write(file + ("/" + task.name + "/linkSpec.xml"))
      task.alignment.toXML.write(file + ("/" + task.name + "/alignment.xml"))
      task.cache.toXML.write(file + ("/" + task.name +  "/cache.xml"))
    }

    def remove(task : LinkingTask)
    {
      (file + ("/" + task.name)).deleteRecursive()
    }
  }
}