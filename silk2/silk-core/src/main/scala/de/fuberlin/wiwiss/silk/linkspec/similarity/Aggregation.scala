package de.fuberlin.wiwiss.silk.linkspec.similarity

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.config.Prefixes

case class Aggregation(required : Boolean, weight : Int, operators : Seq[SimilarityOperator], aggregator : Aggregator) extends SimilarityOperator
{
  /**
   * Computes the similarity between two instances.
   *
   * @param instances The instances to be compared.
   * @param limit The similarity threshold.
   *
   * @return The similarity as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  override def apply(instances : SourceTargetPair[Instance], limit : Double) : Option[Double] =
  {
    val totalWeights = operators.map(_.weight).sum

    val weightedValues =
    {
      for(operator <- operators) yield
      {
        val value = operator(instances, aggregator.computeThreshold(limit, operator.weight.toDouble / totalWeights))
        if(operator.required && value.isEmpty) return None

        (operator.weight, value.getOrElse(-1.0))
      }
    }

    aggregator.evaluate(weightedValues)
  }

  /**
   * Indexes an instance.
   *
   * @param instance The instance to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Instances within the threshold will always get the same index.
   */
  override def index(instance : Instance, threshold : Double) : Set[Seq[Int]] =
  {
    val totalWeights = operators.map(_.weight).sum

    val indexSets =
    {
      for(op <- operators) yield
      {
        val index = op.index(instance, aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights))
        val blockCounts = op.blockCounts(threshold)

        if(op.required && index.isEmpty) return Set.empty;

        (index, blockCounts)
      }
    }

    if(indexSets.isEmpty)
    {
      Set.empty
    }
    else
    {
      val combined = indexSets.reduceLeft[(Set[Seq[Int]], Seq[Int])]
      {
        case ((indexSet1, blockCounts1), (indexSet2, blockCounts2)) =>
        {
          val combinedIndexSet = aggregator.combineIndexes(indexSet1, blockCounts1, indexSet2, blockCounts2)
          val combinedBlockCounts = aggregator.combineBlockCounts(blockCounts1, blockCounts2)

          (combinedIndexSet, combinedBlockCounts)
        }
      }

      combined._1
    }
  }

  /**
   * The number of blocks in each dimension of the index.
   */
  override def blockCounts(threshold : Double) : Seq[Int] =
  {
    val totalWeights = operators.map(_.weight).sum

    operators.map(op => op.blockCounts(aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights)))
             .foldLeft(Seq[Int]())((blockCounts1, blockCounts2) => aggregator.combineBlockCounts(blockCounts1, blockCounts2))
  }

  override def toXML(implicit prefixes : Prefixes) = aggregator match
  {
    case Aggregator(id, params) =>
    {
      <Aggregate required={required.toString} weight={weight.toString} type={id}>
        { operators.map(_.toXML) }
      </Aggregate>
    }
  }
}
