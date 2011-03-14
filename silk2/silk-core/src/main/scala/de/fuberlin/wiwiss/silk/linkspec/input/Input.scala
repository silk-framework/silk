package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.{Elem, Node}

/**
 * An input.
 */
trait Input
{
  /**
   * Retrieves the values of this input for a given instance.
   *
   * @param instances The pair of instances.
   * @return The values.
   */
  def apply(instances : SourceTargetPair[Instance]) : Traversable[String]

  def toXML(implicit prefixes : Prefixes) : Node
}