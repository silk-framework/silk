package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

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
}