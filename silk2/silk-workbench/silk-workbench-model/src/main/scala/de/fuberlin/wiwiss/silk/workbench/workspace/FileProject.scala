package de.fuberlin.wiwiss.silk.workbench.workspace

import xml.XML
import java.io.File
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.util.FileUtils._

class FileProject(file : File) extends Project
{
  private val DataSourceDir = "datasources"
  private val LinkingTaskDir = "linkingtasks"

  override def config =
  {
    val configXML = XML.loadFile(file + "/config.xml")

    val prefixes = (configXML \ "Prefixes" \ "Prefix").map(n => (n \ "@id" text, n \ "@namespace" text)).toMap

    new ProjectConfig(prefixes)
  }

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

  override def modules : Traversable[Module] = synchronized
  {
    //TODO
    Traversable.empty
  }

  override def update(module : Module) : Unit = module match
  {
    case m : DataSourceDefinition => writeDataSource(m, new File(file + "/" + DataSourceDir + "/" + m.name))
    case m : LinkingTask => writeLinkingTask(m, new File(file + "/" + LinkingTaskDir + "/" + m.name))
  }

  override def remove(module : Module) : Unit = module match
  {
    case m : DataSourceDefinition => new File(file + "/" + DataSourceDir + "/" + m.name).deleteRecursive()
    case m : LinkingTask => new File(file + "/" + LinkingTaskDir + "/" + m.name).deleteRecursive()
  }

  private def readDataSource(file : File) =
  {
    val xml = XML.loadFile(new File(file + "/datasource.xml"))

    DataSourceDefinition(file.getName, xml \ "DataSource" text)
  }

  private def writeDataSource(dataSourceDefinition : DataSourceDefinition, file : File)
  {
    val dataSourceXML =
      <DataSource>
      {
        dataSourceDefinition.endpointUri
      }
      </DataSource>

    dataSourceXML.write(new File(file + "/datasource.xml"))
  }

  private def readLinkingTask(file : File) =
  {
    val projectConfig = config

    val linkSpec = LinkSpecification.load(projectConfig.prefixes)(new File(file + "/linkSpec.xml"))

    val alignment = AlignmentReader.readAlignment(new File(file + "/alignment.xml"))

    val cache = Cache.fromXML(XML.loadFile(new File(file + "/cache.xml")))

    LinkingTask(file.getName, linkSpec, alignment, cache)
  }

  private def writeLinkingTask(linkingTask : LinkingTask, file : File)
  {
    linkingTask.linkSpec.toXML.write(new File(file + "/linkSpec.xml"))
    linkingTask.alignment.toXML.write(new File(file + "/alignment.xml"))
    linkingTask.cache.toXML.write(new File(file + "/cache.xml"))
  }
}