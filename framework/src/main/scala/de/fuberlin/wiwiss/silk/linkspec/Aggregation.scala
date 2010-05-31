package de.fuberlin.wiwiss.silk.linkspec

import aggegration.{MinimumAggregation, MaximumAggregation, AverageAggregation}
import de.fuberlin.wiwiss.silk.Instance

trait Aggregation extends Operator
{
    val operators : Traversable[Operator]
}

object Aggregation
{
    def apply(aggType : String, weight : Int, operators : Traversable[Operator]) : Aggregation =
    {
        aggType match
        {
            case "average" => new AverageAggregation(weight, operators)
            case "max" => new MaximumAggregation(weight, operators)
            case "min" => new MinimumAggregation(weight, operators)
            case _ => throw new IllegalArgumentException("Aggregation type unknown: " + aggType)
        }
    }
}