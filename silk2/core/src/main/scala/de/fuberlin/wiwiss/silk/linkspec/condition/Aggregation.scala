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
    val indexSets = operators.map(_.index(instance, threshold))
    aggregator.aggregateIndexes(indexSets)
  }

  override val blockCounts : Seq[Int] =
  {
    val blockCounts = operators.map(_.blockCounts)
    aggregator.aggregateBlockCounts(blockCounts)
  }

  override def toString = aggregator match
  {
    case Aggregator(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
  }
}
