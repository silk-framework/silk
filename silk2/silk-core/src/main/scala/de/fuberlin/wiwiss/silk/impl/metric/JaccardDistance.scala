package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.condition.DistanceMeasure
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "jaccard", label = "Jaccard", description = "Jaccard similarity coefficient.")
class JaccardDistance extends DistanceMeasure
{
  override def apply(values1 : Traversable[String], values2 : Traversable[String], threshold : Double) : Double =
  {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size
    val unionSize = (set1 union set2).size

    1.0 - intersectionSize.toDouble / unionSize
  }
}