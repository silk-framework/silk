package de.fuberlin.wiwiss.silk.workbench.workspace

import xml.XML
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import java.io.{InputStream, File}
import java.util.zip.ZipInputStream
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.evaluation.{Alignment, AlignmentReader}
import de.fuberlin.wiwiss.silk.instance.{Path, Instance, InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import org.xml.sax.InputSource
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

class FileProject(file : File) extends Project
{
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

  override def update(module : Module)
  {
    //TODO
  }

  override def remove(module : Module)
  {
    //TODO
  }

//
//  private class LinkingTaskReader
//  {
//    def read(inputStream : InputStream) : Project =
//    {
//      val zipStream = new ZipInputStream(inputStream)
//      {
//        /*
//         * Ignore close calls in order to read multiple files (XML.load closes the underlying stream after reading)
//         */
//        override def close() {}
//      }
//
//      var linkSpec : LinkSpecification = null
//
//      var alignment : Alignment = null
//
//      var cache : Cache = null
//
//      var currentEntry = zipStream.getNextEntry()
//
//      while(currentEntry != null)
//      {
//        currentEntry.getName match
//        {
//          case "linkspec.xml" => linkSpec = readLinkSpecification(zipStream)
//          case "alignment.xml" => alignment = readAlignment(zipStream)
//          case "cache.xml" => cache = readCache(zipStream)
//        }
//
//        currentEntry = zipStream.getNextEntry()
//      }
//
//      require(datasets != null, "Datasets not found")
//      require(config != null, "Configuration not found")
//      require(alignment != null, "Alignment not found")
//      require(cache != null, "Cache not found")
//
//      inputStream.close()
//
//      new Project(datasets, config, config.linkSpecs.head, alignment, cache)
//    }
//
//    private def readLinkSpecification(inputStream : InputStream) =
//    {
//       ConfigReader.readLinkSpecification(ConfigReader.readXML(new InputSource(inputStream)))
//    }
//
//    private def readAlignment(inputStream : InputStream) =
//    {
//       AlignmentReader.readAlignment(inputStream)
//    }
//
//    private def readCache(inputStream : InputStream) : Cache =
//    {
//      val xml = XML.load(inputStream)
//
//      val prefixes = {for(prefixNode <- xml \ "Prefixes" \ "Prefix") yield (prefixNode \ "@id" text, prefixNode \ "@namespace" text)}.toMap
//
//      val instanceSpecs =
//      {
//        if(xml \ "InstanceSpecifications" isEmpty)
//        {
//          null
//        }
//        else
//        {
//          val sourceSpec = InstanceSpecification.fromXML(xml \ "InstanceSpecifications" \ "Source" \ "_" head)
//          val targetSpec = InstanceSpecification.fromXML(xml \ "InstanceSpecifications" \ "Target" \ "_" head)
//          new SourceTargetPair(sourceSpec, targetSpec)
//        }
//      }
//
//      val positiveInstances =
//      {
//        if(xml \ "PositiveInstances" isEmpty)
//        {
//          null
//        }
//        else
//        {
//          for(pairNode <- xml \ "PositiveInstances" \ "Pair" toList) yield
//          {
//             SourceTargetPair(
//               Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
//               Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
//          }
//        }
//      }
//
//      val negativeInstances =
//      {
//        if(xml \ "NegativeInstances" isEmpty)
//        {
//          null
//        }
//        else
//        {
//          for(pairNode <- xml \ "NegativeInstances" \ "Pair" toList) yield
//          {
//             SourceTargetPair(
//               Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
//               Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
//          }
//        }
//      }
//
//      new Cache(instanceSpecs, positiveInstances, negativeInstances)
//    }
//  }
}