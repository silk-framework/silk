package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

case class LinkCondition(rootAggregation : Aggregation)
{
  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Double =
  {
    rootAggregation(instances, threshold).headOption.getOrElse(0.0)
  }

  def index(instance : Instance, threshold : Double) : Set[Int] =
  {
    val indexes = rootAggregation.index(instance, threshold)

    //Convert the index vectors to scalars
    for(index <- indexes) yield
    {
      (index zip rootAggregation.blockCounts).foldLeft(0){case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight}
    }
  }

  val blockCount =
  {
    rootAggregation.blockCounts.foldLeft(1)(_ * _)
  }
}
