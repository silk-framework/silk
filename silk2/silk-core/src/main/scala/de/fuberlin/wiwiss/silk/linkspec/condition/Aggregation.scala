package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

case class Aggregation(required : Boolean, weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
  override def apply(instances : SourceTargetPair[Instance], threshold : Double) : Option[Double] =
  {
    val weightedValues =
    {
      for(operator <- operators) yield
      {
        val value = operator(instances, threshold)
        if(operator.required && value.isEmpty) return None

        (operator.weight, value.getOrElse(0.0))
      }
    }

    aggregator.evaluate(weightedValues)
  }

  override def index(instance : Instance, threshold : Double) : Set[Seq[Int]] =
  {
    val totalWeights = operators.map(_.weight).sum

    val indexSets =
    {
      for(op <- operators) yield
      {
        val index = op.index(instance, aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights))
        val blockCounts = op.blockCounts

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

  override val blockCounts : Seq[Int] =
  {
    operators.map(_.blockCounts)
             .foldLeft(Seq[Int]())((blockCounts1, blockCounts2) => aggregator.combineBlockCounts(blockCounts1, blockCounts2))
  }

  override def toString = aggregator match
  {
    case Aggregator(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
  }
}
