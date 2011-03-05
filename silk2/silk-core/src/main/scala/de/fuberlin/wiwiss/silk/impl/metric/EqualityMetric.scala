package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.condition.Metric
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "equality", label = "Equality", description = "Return 1 if strings are equal, 0 otherwise.")
class EqualityMetric() extends Metric
{
  private val blockCount = 1000

  override def evaluate(str1 : String, str2 : String, threshold : Double) = if(str1 == str2) 1.0 else 0.0

  override def index(str : String, threshold : Double) : Set[Seq[Int]] =
  {
    Set(Seq((str.hashCode % blockCount).abs))
  }

  override val blockCounts : Seq[Int] =
  {
    Seq(blockCount)
  }
}
