package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import math.abs

/**
 * A Link Condition specifies the conditions which must hold true so that a link is generated between two instances.
 */
case class LinkCondition(rootOperator : Option[Operator])
{
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param threshold The similarity threshold.
   *
   * @return The similarity as a value between 0.0 and 1.0.
   *         Returns 0.0 if the similarity is lower than the threshold.
   *         None, if no similarity could be computed.
   */
  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Double =
  {
    rootOperator match
    {
      case Some(operator) => operator(instances, threshold).getOrElse(0.0)
      case None => 0.0
    }
  }

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  def index(instance : Instance, threshold : Double) : Set[Int] =
  {
    rootOperator match
    {
      case Some(operator) =>
      {
        val indexes = operator.index(instance, threshold)

        //Convert the index vectors to scalars in the range [0, Int.MaxValue]
        for(index <- indexes) yield
        {
          val flatIndex = (index zip operator.blockCounts).foldLeft(0){case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight}

          if(flatIndex == Int.MinValue) 0 else abs(flatIndex)
        }
      }
      case None => Set.empty
    }

  }

  /**
   * The number of blocks in each dimension of the index.
   */
  val blockCount =
  {
    rootOperator match
    {
      case Some(operator) => operator.blockCounts.foldLeft(1)(_ * _)
      case None => 1
    }
  }

  /**
   * Serializes this Link Condition as XML.
   */
  def toXML(implicit prefixes : Prefixes) =
  {
    <LinkCondition>
      { rootOperator.toList.map(_.toXML) }
    </LinkCondition>
  }
}
