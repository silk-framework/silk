package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Strategy, Factory}

trait Metric extends Strategy
{
    def evaluate(value1 : String, value2 : String) : Double
}

object Metric extends Factory[Metric]
