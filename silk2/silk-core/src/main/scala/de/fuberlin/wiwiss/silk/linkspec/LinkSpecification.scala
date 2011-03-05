package de.fuberlin.wiwiss.silk.linkspec

import condition._
import input.{Input, TransformInput, Transformer, PathInput}
import de.fuberlin.wiwiss.silk.instance.Path
import xml.Node
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.util.{Identifier, ValidatingXMLReader, SourceTargetPair}

/**
 * Represents a Silk Link Specification.
 *
 * @param id The id which identifies this link specification. May only contain alphanumeric characters (a - z, 0 - 9).
 */
case class LinkSpecification(id : Identifier,
                             linkType : String,
                             datasets : SourceTargetPair[DatasetSpecification],
                             condition : LinkCondition,
                             filter : LinkFilter,
                             outputs : Traversable[Output])
{
  /**
   * Serializes this Link Specification as XML.
   */
  def toXML : Node =
  {
    <Interlink id={id}>
      <LinkType>{"<" + linkType + ">"}</LinkType>
      { datasets.source.toXML(true) }
      { datasets.target.toXML(false) }
      { condition.toXML }
      { filter.toXML }
      <Outputs>
        { outputs.map(_.toXML) }
      </Outputs>
    </Interlink>
  }
}

object LinkSpecification
{
  private val schemaLocation = "de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd"

  def load(prefixes : Map[String, String]) =
  {
    new ValidatingXMLReader(node => fromXML(node, prefixes), schemaLocation)
  }

  /**
   * Reads a Link Specification from XML.
   */
  def fromXML(node : Node, prefixes : Map[String, String]) : LinkSpecification =
  {
    new LinkSpecification(
      node \ "@id" text,
      resolveQualifiedName(node \ "LinkType" text, prefixes),
      new SourceTargetPair(DatasetSpecification.fromXML(node \ "SourceDataset" head),
                           DatasetSpecification.fromXML(node \ "TargetDataset" head)),
      readLinkCondition(node \ "LinkCondition" head, prefixes),
      LinkFilter.fromXML(node \ "Filter" head),
      (node \ "Outputs" \ "Output").map(Output.fromXML)
    )
  }

  private def readLinkCondition(node : Node, prefixes : Map[String, String]) =
  {
    LinkCondition(readOperators(node.child, prefixes).headOption)
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

  private def readParams(element : Node) : Map[String, String] =
  {
    element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
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
}
