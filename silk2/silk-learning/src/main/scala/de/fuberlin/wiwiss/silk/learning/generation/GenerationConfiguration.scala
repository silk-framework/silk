package de.fuberlin.wiwiss.silk.learning.generation

import xml.Node
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances

case class GenerationConfiguration(linkConditionGenerator: LinkConditionGenerator)

object GenerationConfiguration {
  def fromXml(xml: Node, instances: ReferenceInstances) = {
    GenerationConfiguration(
      linkConditionGenerator = new LinkConditionGenerator(createGenerators(instances))
    )
  }

  private def createGenerators(instances: ReferenceInstances) = {
    (PathPairGenerator(instances) ++ PatternGenerator(instances)).toIndexedSeq
  }
}