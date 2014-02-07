package de.fuberlin.wiwiss.silk.linkagerule

import de.fuberlin.wiwiss.silk.util.{ValidatingXMLReader, DPair}
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import scala.xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.input.Input

/**
 * A transform rule.
 */
case class TransformRule(operator: Option[Input] = None) {
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
   * Serializes this transform rule as XML.
   */
  def toXML(implicit prefixes: Prefixes = Prefixes.empty) = {
    <TransformRule>
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
  def apply(operator: Input): TransformRule = TransformRule(Some(operator))

  def load(resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader)(prefixes, None), "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd")
  }

  /**
   * Reads a transform rule from xml.
   */
  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes, globalThreshold: Option[Double]) = {
    TransformRule(Input.fromXML(node.child, resourceLoader).headOption)
  }
}
