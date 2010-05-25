package de.fuberlin.wiwiss.silk.metric

import de.fuberlin.wiwiss.silk.Instance

trait Metric
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Double
}
