package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import input.Input
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

case class Comparison(required : Boolean, weight : Int, inputs : SourceTargetPair[Input], metric : Metric) extends Operator
{
  override def apply(instances : SourceTargetPair[Instance], threshold : Double) : Option[Double] =
  {
    val set1 = inputs.source(instances)
    val set2 = inputs.target(instances)

    if(!set1.isEmpty && !set2.isEmpty)
    {
      val similarities = for (str1 <- set1; str2 <- set2) yield metric.evaluate(str1, str2, threshold)

      Some(similarities.max)
    }
    else
    {
      None
    }
  }

  override def index(instance : Instance, threshold : Double) : Set[Seq[Int]] =
  {
    val values = inputs.source(SourceTargetPair(instance, instance)) ++ inputs.target(SourceTargetPair(instance, instance))

    values.flatMap(value => metric.index(value, threshold)).toSet
  }

  override val blockCounts = metric.blockCounts

  override def toString = metric match
  {
    case Metric(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
  }
}
