package de.fuberlin.wiwiss.silk.linkagerule

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.linkagerule.input.{Input, PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier, ValidatingXMLReader}

import scala.xml.Node

/**
 * A transform rule.
 */
case class TransformRule(name: Identifier = "transformation", operator: Option[Input] = None, targetProperty: String = "http://silk.wbsg.de/transformed") {
  /**
   * Generates the transformed values.
   *
   * @param entity The source entity.
   *
   * @return The transformed values.
   */
  def apply(entity: Entity): Set[String] = {
    operator match {
      case Some(op) => op(DPair.fill(entity))
      case None => Set.empty
    }
  }

  /**
   * Collects all paths in this rule.
   */
  def paths: Set[Path] = {
    def collectPaths(param: Input): Set[Path] = param match {
      case p: PathInput => Set(p.path)
      case p: TransformInput => p.inputs.flatMap(collectPaths).toSet
    }

    operator match {
      case Some(op) => collectPaths(op)
      case None => Set[Path]()
    }
  }

  /**
   * Serializes this transform rule as XML.
   */
  def toXML(implicit prefixes: Prefixes = Prefixes.empty) = {
    <TransformRule name={name} targetProperty={targetProperty}>
      {operator.toList.map(_.toXML)}
    </TransformRule>
  }
}

/**
 * Creates new transform rules.
 */
object TransformRule {
  /**
   * Creates a new transform rule with one root operator.
   */
  def apply(name: Identifier, operator: Input, targetProperty: String): TransformRule = TransformRule(name, Some(operator), targetProperty)

  def load(resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader)(prefixes, None), "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd")
  }

  /**
   * Reads a transform rule from xml.
   */
  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes, globalThreshold: Option[Double]) = {
    TransformRule(
      name = (node \ "@name").text,
      operator = Input.fromXML(node.child, resourceLoader).headOption,
      targetProperty = (node \ "@targetProperty").text
    )
  }
}
