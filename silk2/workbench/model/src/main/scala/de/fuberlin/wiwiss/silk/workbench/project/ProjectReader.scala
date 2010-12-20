package de.fuberlin.wiwiss.silk.workbench.project

import xml.XML
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import java.io.{InputStream, File}
import java.util.zip.ZipInputStream
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigReader}
import de.fuberlin.wiwiss.silk.evaluation.{Alignment, AlignmentReader}
import de.fuberlin.wiwiss.silk.instance.{Instance, InstanceSpecification, MemoryInstanceCache}

private object ProjectReader
{
  def read(inputStream : InputStream) : Project =
  {
    val zipStream = new ZipInputStream(inputStream)
    {
      /*
       * Ignore close calls in order to read multiple files (XML.load closes the underlying stream after reading)
       */
      override def close() {}
    }

    var datasets : SourceTargetPair[Description] = null

    var config : Configuration = null

    var alignment : Alignment = null

    var cache : Cache = null

    var currentEntry = zipStream.getNextEntry()

    while(currentEntry != null)
    {
      currentEntry.getName match
      {
        case "project.xml" => datasets = readDescription(zipStream)
        case "config.xml" => config = readConfig(zipStream)
        case "alignment.xml" => alignment = readAlignment(zipStream)
        case "cache.xml" => cache = readCache(zipStream)
      }

      currentEntry = zipStream.getNextEntry()
    }

    require(datasets != null, "Datasets not found")
    require(config != null, "Configuration not found")
    require(alignment != null, "Alignment not found")
    require(cache != null, "Cache not found")

    inputStream.close()

    new Project(datasets, config, config.linkSpecs.head, alignment, cache)
  }

  private def readDescription(inputStream : InputStream) =
  {
    val xml = XML.load(inputStream)

    new SourceTargetPair(Description.fromXML(xml \ "Source" \ "Description" head),
                         Description.fromXML(xml \ "Target" \ "Description" head))
  }

  private def readConfig(inputStream : InputStream) =
  {
     ConfigReader.read(inputStream)
  }

  private def readAlignment(inputStream : InputStream) =
  {
     AlignmentReader.readAlignment(inputStream)
  }

  private def readCache(inputStream : InputStream) : Cache =
  {
    val xml = XML.load(inputStream)

    val instanceSpecs =
    {
      if(xml \ "InstanceSpecifications" isEmpty)
      {
        null
      }
      else
      {
        val sourceInstanceSpec = InstanceSpecification.fromXML(xml \ "InstanceSpecifications" \ "Source" \ "_" head)
        val targetInstanceSpec = InstanceSpecification.fromXML(xml \ "InstanceSpecifications" \ "Target" \ "_" head)

        new SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
      }
    }

    val positiveInstances =
    {
      if(xml \ "PositiveInstances" isEmpty)
      {
        null
      }
      else
      {
        for(pairNode <- xml \ "PositiveInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head))
        }
      }
    }

    val negativeInstances =
    {
      if(xml \ "NegativeInstances" isEmpty)
      {
        null
      }
      else
      {
        for(pairNode <- xml \ "NegativeInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head))
        }
      }
    }

    new Cache(instanceSpecs, positiveInstances, negativeInstances)
  }
}






