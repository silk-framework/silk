package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

class LinkCondition(val rootAggregation : Aggregation)
{
    def apply(sourceInstance : Instance, targetInstance : Instance) : Double =
    {
        rootAggregation(sourceInstance, targetInstance).headOption.getOrElse(0.0)
    }
}
