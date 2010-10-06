package de.fuberlin.wiwiss.silk.config

import xml._
import javax.xml.transform.stream.StreamSource
import de.fuberlin.wiwiss.silk.linkspec._
import input.{Input, TransformInput, Transformer, PathInput}
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.output.{AlignmentWriter, Output}
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import parsing.NoBindingFactoryAdapter
import javax.xml.validation.SchemaFactory
import org.xml.sax.SAXException
import java.io.{FileInputStream, File, InputStream}
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}

object ConfigLoader
{
    def load(file : File) : Configuration =
    {
        try
        {
            load(new FileInputStream(file))
        }
        catch
        {
            case ex : ValidationException =>
                throw new ValidationException("Could not load configuration file: " + file + ". " + ex.getMessage, ex.getCause)
        }
    }

    def load(inputStream : InputStream) : Configuration =
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

            val prefixes = loadPrefixes(xml)
            val sources = loadDataSources(xml)
            val linkSpecifications = loadLinkSpecifications(xml, prefixes, sources.map(s => (s.id, s)).toMap)
            val outputs = loadOutputs(xml \ "Outputs" \ "Output")

            new Configuration(prefixes, sources, linkSpecifications, outputs)
        }
        finally
        {
            inputStream.close()
        }
    }

    private def loadPrefixes(xml : Elem) : Map[String, String] =
    {
        xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
    }

    private def loadDataSources(xml : Elem) : Traversable[Source] =
    {
        (xml \ "DataSources" \ "DataSource").map(ds => new Source(ds \ "@id" text, DataSource(ds \ "@type" text, loadParams(ds))))
    }

    private def loadParams(element : Node) : Map[String, String] =
    {
        element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
    }

    private def loadLinkSpecifications(node : Node, prefixes : Map[String, String], sourceMap : Map[String, Source]) : Traversable[LinkSpecification] =
    {
        (node \ "Interlinks" \ "Interlink").map(p => loadLinkSpecification(p, prefixes, sourceMap))
    }

    private def loadLinkSpecification(node : Node, prefixes : Map[String, String], sourceMap : Map[String, Source]) : LinkSpecification =
    {
        new LinkSpecification(
            node \ "@id" text,
            resolveQualifiedName(node \ "LinkType" text, prefixes),
            loadDatasetSpecification(node \ "SourceDataset", sourceMap),
            loadDatasetSpecification(node \ "TargetDataset", sourceMap),
            (node \ "Blocking").headOption.map(blockingNode => loadBlocking(blockingNode)),
            new LinkCondition(loadAggregation(node \ "LinkCondition" \ "Aggregate" head)),
            loadLinkFilter(node \ "Filter" head),
            loadOutputs(node \ "Outputs" \ "Output")
        )
    }

    private def loadDatasetSpecification(node : NodeSeq, sourceMap : Map[String, Source]) : DatasetSpecification =
    {
        val datasourceName = node \ "@dataSource" text
        
        new DatasetSpecification(
            sourceMap.get(datasourceName).getOrElse(throw new ValidationException("Datasource " + datasourceName + " not defined.")),
            node \ "@var" text,
            (node \ "RestrictTo").text.trim 
            )
    }

    private def loadOperators(nodes : Seq[Node]) : Traversable[Operator] =
    {
        nodes.collect
        {
            case node @ <Aggregate>{_*}</Aggregate> => loadAggregation(node)
            case node @ <Compare>{_*}</Compare> => loadComparison(node)
        }
    }

    private def loadAggregation(node : Node) : Aggregation =
    {
        val requiredStr = node \ "@required" text
        val weightStr = node \ "@weight" text

        val aggregator = Aggregator(node \ "@type" text, loadParams(node))

        new Aggregation(
            if(requiredStr.isEmpty) false else requiredStr.toBoolean,
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadOperators(node.child),
            aggregator
        )
    }

    private def loadComparison(node : Node) : Comparison =
    {
        val requiredStr = node \ "@required" text
        val weightStr = node \ "@weight" text
        val metric = Metric(node \ "@metric" text, loadParams(node))

        new Comparison(
            if(requiredStr.isEmpty) false else requiredStr.toBoolean,
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadInputs(node.child),
            metric
        )
    }

    private def loadBlocking(node : Node) : Blocking =
    {
        new Blocking(
            loadInputs(node.child).asInstanceOf[Traversable[PathInput]],
            BlockingFunction(node \ "@function" text, loadParams(node)),
            (node \ "@blocks").text.toInt,
            (node \ "@overlap").headOption.map(_.text.toDouble).getOrElse(0.0)
        )
    }

    private def loadInputs(nodes : Seq[Node]) : Seq[Input] =
    {
        nodes.collect {
            case p @ <Input/> =>
            {
                val pathStr = p \ "@path" text
                val path = Path.parse(pathStr)
                PathInput(path)
            }
            case p @ <TransformInput>{_*}</TransformInput> =>
            {
                val transformer = Transformer(p \ "@function" text, loadParams(p))
                TransformInput(loadInputs(p.child), transformer)
            }
        }
    }

    private def loadLinkFilter(node : Node) =
    {
        val limitStr = (node \ "@limit").text
        new LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
    }

    private def loadOutputs(nodes : NodeSeq) : Traversable[Output] =
    {
        nodes.map(loadOutput)
    }

    private def loadOutput(node : Node) : Output =
    {
        new Output(
            writer = AlignmentWriter(node \ "@type" text, loadParams(node)),
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
