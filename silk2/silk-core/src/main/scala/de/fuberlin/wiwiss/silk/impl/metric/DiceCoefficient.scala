package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.condition.SimilarityMeasure

@StrategyAnnotation(id = "jaccard", label = "Jaccard", description = "Jaccard similarity coefficient.")
class DiceCoefficient extends SimilarityMeasure
{
  override def apply(values1 : Traversable[String], values2 : Traversable[String], threshold : Double) : Double =
  {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size * 2
    val totalSize = set1.size + set2.size

    intersectionSize.toDouble / totalSize
  }
}