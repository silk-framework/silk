package de.fuberlin.wiwiss.silk.linkspec

import similarity._
import input.{Input, TransformInput, Transformer, PathInput}
import de.fuberlin.wiwiss.silk.instance.Path
import xml.Node
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util._
import java.util.logging.Logger

/**
 * Represents a Silk Link Specification.
 *
 * @param id The id which identifies this link specification. By default a new random identifier is generated.
 * @param linkType The type of link to be generated. Defaults to owl:sameAs.
 */
case class LinkSpecification(id: Identifier = Identifier.random,
                             linkType: Uri = Uri.fromURI("http://www.w3.org/2002/07/owl#sameAs"),
                             datasets: SourceTargetPair[DatasetSpecification] = SourceTargetPair.fill(DatasetSpecification.empty),
                             rule: LinkageRule = LinkageRule(),
                             filter: LinkFilter = LinkFilter(),
                             outputs: Traversable[Output] = Traversable.empty) {
  /**
   * Serializes this Link Specification as XML.
   */
  def toXML(implicit prefixes: Prefixes): Node = {
    <Interlink id={id}>
      <LinkType>{linkType.toTurtle}</LinkType>
      {datasets.source.toXML(true)}{datasets.target.toXML(false)}{rule.toXML}{filter.toXML}
      <Outputs>
      {outputs.map(_.toXML)}
      </Outputs>
    </Interlink>
  }
}

object LinkSpecification {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd"

  private val logger = Logger.getLogger(LinkSpecification.getClass.getName)

  def load(implicit prefixes: Prefixes) = {
    new ValidatingXMLReader(node => fromXML(node), schemaLocation)
  }

  /**
   * Reads a Link Specification from XML.
   */
  def fromXML(node: Node)(implicit prefixes: Prefixes): LinkSpecification = {
    //Read id
    val id = (node \ "@id").text

    //Read linkage rule node
    val linkConditionNode = (node \ "LinkCondition").headOption
    val linkageRuleNode = (node \ "LinkageRule").headOption

    if(linkageRuleNode.isEmpty && linkConditionNode.isEmpty) throw new ValidationException("No <LinkageRule> found in link specification with id '" + id + "'")
    if(linkConditionNode.isDefined) logger.warning("<LinkCondition> has been renamed to <LinkageRule>. Please update the link specification.")

    //Read filter
    val filter = LinkFilter.fromXML(node \ "Filter" head)
    implicit val globalThreshold = filter.threshold

    new LinkSpecification(
      id,
      resolveQualifiedName((node \ "LinkType").text.trim, prefixes),
      new SourceTargetPair(DatasetSpecification.fromXML(node \ "SourceDataset" head),
      DatasetSpecification.fromXML(node \ "TargetDataset" head)),
      readLinkageRule(linkageRuleNode.getOrElse(linkConditionNode.get)),
      filter,
      (node \ "Outputs" \ "Output").map(Output.fromXML)
    )
  }

  private def readLinkageRule(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]) = {
    LinkageRule(readOperators(node.child).headOption)
  }

  private def readOperators(nodes: Seq[Node])(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Seq[SimilarityOperator] = {
    nodes.collect {
      case node@ <Aggregate>{_*}</Aggregate> => readAggregation(node)
      case node@ <Compare>{_*}</Compare> => readComparison(node)
    }
  }

  private def readAggregation(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Aggregation = {
    val requiredStr = node \ "@required" text
    val weightStr = node \ "@weight" text

    val aggregator = Aggregator(node \ "@type" text, readParams(node))

    Aggregation(
      id = Operator.readId(node),
      required = if (requiredStr.isEmpty) false else requiredStr.toBoolean,
      weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
      operators = readOperators(node.child),
      aggregator = aggregator
    )
  }

  private def readComparison(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Comparison = {
    val id = Operator.readId(node)
    val inputs = readInputs(node.child)
    if(inputs.size != 2) throw new ValidationException("A comparison must have exactly 2 inputs ", id, "Comparison")

    try {
      val requiredStr = node \ "@required" text
      val threshold = (node \ "@threshold").headOption.map(_.text.toDouble).getOrElse(1.0 - globalThreshold.getOrElse(1.0))
      val weightStr = node \ "@weight" text
      val metric = DistanceMeasure(node \ "@metric" text, readParams(node))

      Comparison(
        id = id,
        required = if (requiredStr.isEmpty) false else requiredStr.toBoolean,
        threshold = threshold,
        weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
        inputs = SourceTargetPair(inputs(0), inputs(1)),
        metric = metric
      )
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Comparison")
    }
  }

  private def readInputs(nodes: Seq[Node])(implicit prefixes: Prefixes): Seq[Input] = {
    nodes.collect {
      case node @ <Input/> => readPathInput(node)
      case node @ <TransformInput>{_*}</TransformInput> => readTransformInput(node)
    }
  }

  private def readPathInput(node: Node)(implicit prefixes: Prefixes) = {
    val id = Operator.readId(node)

    try {
      val pathStr = (node \ "@path").text
      val path = Path.parse(pathStr)
      PathInput(id, path)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Path")
    }
  }

  private def readTransformInput(node: Node)(implicit prefixes: Prefixes) = {
    val id = Operator.readId(node)
    val inputs = readInputs(node.child)
    if(inputs.isEmpty) throw new ValidationException("No input defined", id, "Transformation")

    try {
      val transformer = Transformer(node \ "@function" text, readParams(node))
      TransformInput(id, inputs, transformer)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Tranformation")
    }
  }

  private def readParams(element: Node): Map[String, String] = {
    (element \ "Param").map(p => (p \ "@name" text, p \ "@value" text)).toMap
  }

  private def resolveQualifiedName(name: String, prefixes: Map[String, String]) = {
    if (name.startsWith("<") && name.endsWith(">")) {
      name.substring(1, name.length - 1)
    }
    else {
      name.split(":", 2) match {
        case Array(prefix, suffix) => prefixes.get(prefix) match {
          case Some(resolvedPrefix) => resolvedPrefix + suffix
          case None => throw new ValidationException("Unknown prefix: " + prefix)
        }
        case _ => throw new ValidationException("No prefix found in " + name)
      }
    }
  }
}
