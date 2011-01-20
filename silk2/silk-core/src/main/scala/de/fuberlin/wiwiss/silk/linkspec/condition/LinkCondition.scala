package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

case class LinkCondition(rootOperator : Option[Operator])
{
  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Double =
  {
    rootOperator match
    {
      case Some(operator) => operator(instances, threshold).getOrElse(0.0)
      case None => 0.0
    }
  }

  def index(instance : Instance, threshold : Double) : Set[Int] =
  {
    rootOperator match
    {
      case Some(operator) =>
      {
        val indexes = operator.index(instance, threshold)

        //Convert the index vectors to scalars
        for(index <- indexes) yield
        {
          (index zip operator.blockCounts).foldLeft(0){case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight}
        }
      }
      case None => Set.empty
    }

  }

  val blockCount =
  {
    rootOperator match
    {
      case Some(operator) => operator.blockCounts.foldLeft(1)(_ * _)
      case None => 1
    }
  }
}
