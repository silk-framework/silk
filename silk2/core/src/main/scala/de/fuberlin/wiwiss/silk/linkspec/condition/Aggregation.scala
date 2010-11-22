package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class Aggregation(required : Boolean, weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
  override def apply(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double] =
  {
    val weightedValues = operators.flatMap{operator =>
        val values = operator(sourceInstance, targetInstance)
        if(operator.required && values.isEmpty) return Traversable.empty
        values.map(value => (operator.weight, value))
    }

    aggregator.evaluate(weightedValues).toList
  }

  override def index(instance : Instance) : Traversable[Double] =
  {
    operators.flatMap(_.index(instance))
  }

  override def toString = aggregator match
  {
    case Aggregator(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
  }
}
