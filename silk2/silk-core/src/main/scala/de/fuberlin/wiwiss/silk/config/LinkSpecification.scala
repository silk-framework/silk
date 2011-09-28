package de.fuberlin.wiwiss.silk.config

import xml.Node
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.util._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

/**
 * Represents a Silk Link Specification.
 *
 * @param id The id which identifies this link specification. By default a new random identifier is generated.
 * @param linkType The type of link to be generated. Defaults to owl:sameAs.
 */
case class LinkSpecification(id: Identifier = Identifier.random,
                             linkType: Uri = Uri.fromURI("http://www.w3.org/2002/07/owl#sameAs"),
                             datasets: DPair[DatasetSpecification] = DPair.fill(DatasetSpecification.empty),
                             rule: LinkageRule = LinkageRule(),
                             filter: LinkFilter = LinkFilter(),
                             outputs: Traversable[Output] = Traversable.empty) {
  /**
   * Serializes this Link Specification as XML.
   */
  def toXML(implicit prefixes: Prefixes): Node = {
    <Interlink id={id}>
      <LinkType>{linkType.toTurtle}</LinkType>
      {datasets.source.toXML(true)}
      {datasets.target.toXML(false)}
      {rule.toXML}
      {filter.toXML}
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
      new DPair(DatasetSpecification.fromXML(node \ "SourceDataset" head),
      DatasetSpecification.fromXML(node \ "TargetDataset" head)),
      LinkageRule.fromXML(linkageRuleNode.getOrElse(linkConditionNode.get)),
      filter,
      (node \ "Outputs" \ "Output").map(Output.fromXML)
    )
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
