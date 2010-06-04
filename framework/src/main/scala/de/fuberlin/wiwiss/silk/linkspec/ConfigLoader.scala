package de.fuberlin.wiwiss.silk.linkspec

import javax.xml.validation.SchemaFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import xml._
import java.io.{File}
import org.xml.sax.{SAXException}
import javax.xml.transform.stream.StreamSource

object ConfigLoader
{
    def load(file : File) : Configuration =
    {
        validateXML(file)
        val xml = XML.loadFile(file)

        val prefixes = loadPrefixes(xml)
        val dataSources = loadDataSources(xml)
        val linkSpecifications = loadLinkSpecifications(xml, dataSources)

        new Configuration(prefixes, dataSources, linkSpecifications)
    }

    private def validateXML(file : File) : Unit =
    {
        try {
            val factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
            val stream = ClassLoader.getSystemClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd")
            val schema = factory.newSchema(new StreamSource(stream))
            val validator = schema.newValidator()
            validator.validate(new StreamSource(file))
        } catch {
            case ex : SAXException => throw new ValidationException("Invalid XML. Details: " + ex.getMessage, ex)
        }

    }

    private def loadPrefixes(xml : Elem) : Map[String, String] =
    {
        xml \ "Prefixes" \ "Prefix" map(prefix => (prefix \ "@id" text, prefix \ "@namespace" text)) toMap
    }

    private def loadDataSources(xml : Elem) : Map[String, DataSource] =
    {
        (xml \ "DataSources" \ "DataSource")
            .map(ds => DataSource(ds \ "@type" text, ds \ "@id" text, loadParams(ds)))
            .map(ds => (ds.id, ds)).toMap
    }

    private def loadParams(element : NodeSeq) : Map[String, String] =
    {
        element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
    }

    private def loadLinkSpecifications(node : Node, dataSources : Map[String, DataSource]) : Map[String, LinkSpecification] =
    {
        node \ "Interlinks" \ "Interlink" map(p => (p \ "@id" text, loadLinkSpecification(p, dataSources))) toMap
    }

    private def loadLinkSpecification(node : Node, dataSources : Map[String, DataSource]) : LinkSpecification =
    {
        new LinkSpecification(
            node \ "LinkType" text,
            loadDatasetSpecification(node \ "SourceDataset", dataSources),
            loadDatasetSpecification(node \ "TargetDataset", dataSources),
            loadAggregation(node \ "LinkCondition" \ "Aggregate" head),
            (node \ "Thresholds" \ "@accept" text).toDouble,
            (node \ "Thresholds" \ "@verify").map(_.text.toDouble).headOption.getOrElse(0.0),
            if (node \ "Limit" isEmpty) null else LinkLimit((node \ "Limit" \ "@max" text).toInt, node \ "Limit" \ "@method" text),
            loadOutputs(node \ "Outputs" \ "Output")
            )
    }

    private def loadDatasetSpecification(node : NodeSeq, dataSources : Map[String, DataSource]) : DatasetSpecification =
    {
        val datasourceName = node \ "@dataSource" text
        
        new DatasetSpecification(
            dataSources.get(datasourceName).getOrElse(throw new ValidationException("Datasource " + datasourceName + "not defined.")),
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
        val weightStr = node \ "@weight" text

        val aggregator = Aggregator(node \ "@type" text, loadParams(node.child))

        new Aggregation(
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadOperators(node.child),
            aggregator
        )
    }

    private def loadComparison(node : Node) : Comparison =
    {
        val weightStr = node \ "@weight" text
        val metric = Metric(node \ "@metric" text, loadParams(node.child))

        new Comparison(
            if(weightStr.isEmpty) 1 else weightStr.toInt,
            loadInputs(node.child),
            metric
        )
    }

    private def loadInputs(nodes : Seq[Node]) : Seq[Input] =
    {
        nodes.collect {
            case p @ <Input/> => new PathInput(p \ "@path" text)
            case p @ <TransformInput>{_*}</TransformInput> => TransformInput(p \ "@function" text, loadInputs(p.child), loadParams(p.child))
        }
    }

    private def loadOutputs(nodes : NodeSeq) : Traversable[Output] =
    {
        nodes.map(loadOutput)
    }

    private def loadOutput(node : Node) : Output =
    {
        Output(node \ "@type" text, loadParams(node))
    }
}