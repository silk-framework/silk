package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class Aggregation(required : Boolean, weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
  override def apply(sourceInstance : Instance, targetInstance : Instance, threshold : Double) : Traversable[Double] =
  {
    val weightedValues = operators.flatMap{operator =>
        val values = operator(sourceInstance, targetInstance, threshold)
        if(operator.required && values.isEmpty) return Traversable.empty
        values.map(value => (operator.weight, value))
    }

    aggregator.evaluate(weightedValues).toList
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
