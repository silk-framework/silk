package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric

class EqualityMetric(val params : Map[String, String] = Map.empty) extends Metric
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
