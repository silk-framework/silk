package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.Operator

/**
 * An input.
 */
trait Input extends Operator {
  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  def apply(entities: DPair[Entity]): Set[String]

  def toXML(implicit prefixes: Prefixes): Node
}