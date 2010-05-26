package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Metric
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Double
}

class StringEqualityMetric(params : Traversable[Param]) extends Metric
{

}