package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.condition.DistanceMeasure

@StrategyAnnotation(id = "dice", label = "Dice coefficient", description = "Dice similarity coefficient.")
class DiceCoefficient extends DistanceMeasure
{
  override def apply(values1 : Traversable[String], values2 : Traversable[String], threshold : Double) : Double =
  {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size * 2
    val totalSize = set1.size + set2.size

    1.0 - intersectionSize.toDouble / totalSize
  }
}