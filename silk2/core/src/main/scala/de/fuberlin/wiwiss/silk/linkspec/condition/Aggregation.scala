package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class Aggregation(required : Boolean, weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
  override def apply(sourceInstance : Instance, targetInstance : Instance, threshold : Double) : Option[Double] =
  {
    val weightedValues =
    {
      for(operator <- operators) yield
      {
        val value = operator(sourceInstance, targetInstance, threshold)
        if(operator.required && value.isEmpty) return None

        (operator.weight, value.getOrElse(0.0))
      }
    }

    aggregator.evaluate(weightedValues)
  }

  override def index(instance : Instance, threshold : Double) : Set[Seq[Int]] =
  {
    //TODO modify threshold
    val indexSets = for(op <- operators) yield (op.index(instance, threshold), op.blockCounts)

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

  override val blockCounts : Seq[Int] =
  {
    operators.map(_.blockCounts)
             .reduceLeft((blockCounts1, blockCounts2) => aggregator.combineBlockCounts(blockCounts1, blockCounts2))
  }

  override def toString = aggregator match
  {
    case Aggregator(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
  }
}
