package de.fuberlin.wiwiss.silk.learning.generation

import xml.Node
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances

case class GenerationConfiguration(pathPairs: Traversable[SourceTargetPair[Path]])

object GenerationConfiguration {
  def fromXml(xml: Node, instances: ReferenceInstances) = {
    GenerationConfiguration(
      pathPairs = PathPairFinder(instances)
    )
  }
}