package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class LinkCondition(val rootAggregation : Aggregation)
{
  def apply(sourceInstance : Instance, targetInstance : Instance) : Double =
  {
    rootAggregation(sourceInstance, targetInstance).headOption.getOrElse(0.0)
  }

  def index(instance : Instance) =
  {
    rootAggregation.index(instance)
  }
}
