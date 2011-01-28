package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Configuration
import xml.{PrettyPrinter, Node}
import java.io.OutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}
import de.fuberlin.wiwiss.silk.evaluation.Alignment

private object ProjectWriter
{
  private type XmlWriter = (Node => Unit)

  def write(project : Project, outputStream : OutputStream)
  {
    val zipStream = new ZipOutputStream(outputStream)
    val xmlWriter = writeXML(zipStream) _

    zipStream.putNextEntry(new ZipEntry("project.xml"))
    writeDescription(project.desc, xmlWriter)
    zipStream.closeEntry()

    zipStream.putNextEntry(new ZipEntry("config.xml"))
    writeConfig(project.config, xmlWriter)
    zipStream.closeEntry()

    zipStream.putNextEntry(new ZipEntry("alignment.xml"))
    writeAlignment(project.alignment, xmlWriter)
    zipStream.closeEntry()

    zipStream.putNextEntry(new ZipEntry("cache.xml"))
    writeCache(project.cache, xmlWriter)
    zipStream.closeEntry()

    zipStream.close()
  }

  private def writeDescription(desc : SourceTargetPair[Description], writer : XmlWriter)
  {
    writer
    {
      <Project>
        <Source>
          {desc.source.toXML}
        </Source>
        <Target>
          {desc.target.toXML}
        </Target>
      </Project>
    }
  }

  private def writeConfig(config : Configuration, writer : XmlWriter)
  {
    writer
    {
      config.toXML
    }
  }

  private def writeAlignment(alignment : Alignment, writer : XmlWriter)
  {
    writer
    {
      alignment.toXML
    }
  }

  private def writeCache(cache : Cache, writer : XmlWriter)
  {
    writer
    {
      cache.toXML
    }
  }

  private def writeXML(outputStream : OutputStream)(xml : Node)
  {
    outputStream.write(new PrettyPrinter(140, 2).format(xml).getBytes("UTF-8"))
  }
}
