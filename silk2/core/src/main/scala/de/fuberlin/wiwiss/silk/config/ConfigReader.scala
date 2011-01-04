package de.fuberlin.wiwiss.silk.config

import xml._
import javax.xml.transform.stream.StreamSource
import de.fuberlin.wiwiss.silk.linkspec._
import condition.Blocking
import input.{Input, TransformInput, Transformer, PathInput}
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.output.{LinkWriter, Output}
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import parsing.NoBindingFactoryAdapter
import javax.xml.validation.SchemaFactory
import org.xml.sax.SAXException
import java.io.{FileInputStream, File, InputStream}
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * Reads a Silk Configuration.
 */
object ConfigReader
{
  def read(file : File) : Configuration =
  {
    try
    {
      read(new FileInputStream(file))
    }
    catch
    {
      case ex : ValidationException =>
        throw new ValidationException("Could not read configuration file: " + file + ". " + ex.getMessage, ex.getCause)
    }
  }

  def read(inputStream : InputStream) : Configuration =
  {
    try
    {
      val xml =
        try
        {
          new ValidatingFactoryAdapter().loadXML(new InputSource(inputStream))
        }
        catch
        {
          case ex : SAXException => throw new ValidationException("Invalid XML. Details: " + ex.getMessage, ex)
        }

      val prefixes = readPrefixes(xml)
      val sources = readDataSources(xml)
      val linkSpecifications = readLinkSpecifications(xml, prefixes, sources.map(s => (s.id, s)).toMap)
      val outputs = readOutputs(xml \ "Outputs" \ "Output")

      new Configuration(prefixes, sources, linkSpecifications, outputs)
    }
    finally
    {
      inputStream.close()
    }
  }

  private def readPrefixes(xml : Elem) : Map[String, String] =
  {
    xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
  }

  private def readDataSources(xml : Elem) : Traversable[Source] =
  {
    (xml \ "DataSources" \ "DataSource").map(ds => new Source(ds \ "@id" text, DataSource(ds \ "@type" text, readParams(ds))))
  }

  private def readParams(element : Node) : Map[String, String] =
  {
    element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
  }

  private def readLinkSpecifications(node : Node, prefixes : Map[String, String], sourceMap : Map[String, Source]) : Traversable[LinkSpecification] =
  {
    (node \ "Interlinks" \ "Interlink").map(p => readLinkSpecification(p, prefixes, sourceMap))
  }

  private def readLinkSpecification(node : Node, prefixes : Map[String, String], sourceMap : Map[String, Source]) : LinkSpecification =
  {
    new LinkSpecification(
      node \ "@id" text,
      resolveQualifiedName(node \ "LinkType" text, prefixes),
      new SourceTargetPair(readDatasetSpecification(node \ "SourceDataset", sourceMap),
                           readDatasetSpecification(node \ "TargetDataset", sourceMap)),
      (node \ "Blocking").headOption.map(blockingNode => readBlocking(blockingNode)),
      readLinkCondition(node \ "LinkCondition" head, prefixes),
      readLinkFilter(node \ "Filter" head),
      readOutputs(node \ "Outputs" \ "Output")
    )
  }

  private def readDatasetSpecification(node : NodeSeq, sourceMap : Map[String, Source]) : DatasetSpecification =
  {
    val datasourceName = node \ "@dataSource" text

    new DatasetSpecification(
      sourceMap.get(datasourceName).getOrElse(throw new ValidationException("Datasource " + datasourceName + " not defined.")),
      node \ "@var" text,
      (node \ "RestrictTo").text.trim
    )
  }

  def readLinkCondition(node : Node, prefixes : Map[String, String]) =
  {
    new LinkCondition(readAggregation(node \ "Aggregate" head, prefixes))
  }

  private def readOperators(nodes : Seq[Node], prefixes : Map[String, String]) : Traversable[Operator] =
  {
    nodes.collect
    {
      case node @ <Aggregate>{_*}</Aggregate> => readAggregation(node, prefixes)
      case node @ <Compare>{_*}</Compare> => readComparison(node, prefixes)
    }
  }

  private def readAggregation(node : Node, prefixes : Map[String, String]) : Aggregation =
  {
    val requiredStr = node \ "@required" text
    val weightStr = node \ "@weight" text

    val aggregator = Aggregator(node \ "@type" text, readParams(node))

    new Aggregation(
      if(requiredStr.isEmpty) false else requiredStr.toBoolean,
      if(weightStr.isEmpty) 1 else weightStr.toInt,
      readOperators(node.child, prefixes),
      aggregator
    )
  }

  private def readComparison(node : Node, prefixes : Map[String, String]) : Comparison =
  {
    val requiredStr = node \ "@required" text
    val weightStr = node \ "@weight" text
    val metric = Metric(node \ "@metric" text, readParams(node))
    val inputs = readInputs(node.child, prefixes)

    new Comparison(
      if(requiredStr.isEmpty) false else requiredStr.toBoolean,
      if(weightStr.isEmpty) 1 else weightStr.toInt,
      SourceTargetPair(inputs(0), inputs(1)),
      metric
    )
  }

  private def readBlocking(node : Node) : Blocking =
  {
    new Blocking(
      (node \ "@blocks").headOption.map(_.text.toInt).getOrElse(1000),
      (node \ "@overlap").headOption.map(_.text.toDouble).getOrElse(0.4)
    )
  }

  private def readInputs(nodes : Seq[Node], prefixes : Map[String, String]) : Seq[Input] =
  {
    nodes.collect {
      case p @ <Input/> =>
      {
        val pathStr = p \ "@path" text
        val path = Path.parse(pathStr, prefixes)
        PathInput(path)
      }
      case p @ <TransformInput>{_*}</TransformInput> =>
      {
        val transformer = Transformer(p \ "@function" text, readParams(p))
        TransformInput(readInputs(p.child, prefixes), transformer)
      }
    }
  }

  private def readLinkFilter(node : Node) =
  {
    val limitStr = (node \ "@limit").text
    new LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
  }

  private def readOutputs(nodes : NodeSeq) : Traversable[Output] =
  {
    nodes.map(readOutput)
  }

  private def readOutput(node : Node) : Output =
  {
    new Output(
      writer = LinkWriter(node \ "@type" text, readParams(node)),
      minConfidence = (node \ "@minConfidence").headOption.map(_.text.toDouble),
      maxConfidence = (node \ "@maxConfidence").headOption.map(_.text.toDouble)
    )
  }

  private def resolveQualifiedName(name : String, prefixes : Map[String, String]) =
  {
    if(name.startsWith("<") && name.endsWith(">"))
    {
      name.substring(1, name.length - 1)
    }
    else
    {
      name.split(":", 2) match
      {
        case Array(prefix, suffix) => prefixes.get(prefix) match
        {
          case Some(resolvedPrefix) => resolvedPrefix + suffix
          case None => throw new IllegalArgumentException("Unknown prefix: " + prefix)
        }
        case _ => throw new IllegalArgumentException("No prefix found in " + name)
      }
    }
  }

  class ValidatingFactoryAdapter extends NoBindingFactoryAdapter
  {
    def loadXML(source : InputSource) : Elem =
    {
      //Load XML Schema
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schemaStream = getClass().getClassLoader().getResourceAsStream("de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd")
      if(schemaStream == null) throw new ValidationException("XML Schema for Link Specification not found")
      val schema = schemaFactory.newSchema(new StreamSource(schemaStream))

      //Create parser
      val parserFactory = SAXParserFactory.newInstance()
      parserFactory.setNamespaceAware(true)
      parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      val parser = parserFactory.newSAXParser()

      val xr = parser.getXMLReader()
      val vh = schema.newValidatorHandler()
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      // parse file
      scopeStack.push(TopScope)
      xr.parse(source)
      scopeStack.pop
      return rootElem.asInstanceOf[Elem]
    }
  }
}
