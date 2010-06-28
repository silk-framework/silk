package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait Aggregator extends Strategy
{
    def evaluate(weightedValues : Traversable[(Int, Double)]) : Option[Double]
}

object Aggregator extends Factory[Aggregator]
