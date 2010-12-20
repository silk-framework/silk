package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.{ConfigWriter, Configuration}
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.evaluation.AlignmentWriter
import xml.{NodeBuffer, PrettyPrinter, Node}
import java.io.{OutputStream, FileOutputStream, OutputStreamWriter}
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
      ConfigWriter.serializeConfig(config)
    }
  }

  private def writeAlignment(alignment : Alignment, writer : XmlWriter)
  {
    writer
    {
      AlignmentWriter.serialize(alignment)
    }
  }

  private def writeCache(cache : Cache, writer : XmlWriter)
  {
    writer
    {
      val nodes = new NodeBuffer()

      if(cache.instanceSpecs != null)
      {
        nodes.append(
          <InstanceSpecifications>
            <Source>{cache.instanceSpecs.source.toXML}</Source>
            <Target>{cache.instanceSpecs.target.toXML}</Target>
          </InstanceSpecifications>)
      }

      if(cache.positiveInstances != null)
      {
        nodes.append(
            <PositiveInstances>{
            for(SourceTargetPair(sourceInstance, targetInstance) <- cache.positiveInstances) yield
            {
              <Pair>
                <Source>{sourceInstance.toXML}</Source>
                <Target>{targetInstance.toXML}</Target>
              </Pair>
            }
            }</PositiveInstances>)
      }

      if(cache.negativeInstances != null)
      {
        nodes.append(
          <NegativeInstances>{
            for(SourceTargetPair(sourceInstance, targetInstance) <- cache.negativeInstances) yield
            {
              <Pair>
                <Source>{sourceInstance.toXML}</Source>
                <Target>{targetInstance.toXML}</Target>
              </Pair>
            }
          }</NegativeInstances>)
      }

      <Cache>{nodes}</Cache>
    }
  }

  private def writeXML(outputStream : OutputStream)(xml : Node)
  {
    outputStream.write(new PrettyPrinter(140, 2).format(xml).getBytes("UTF-8"))
  }
}