package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.{Param, Plugin, PluginCategories}

@Plugin(
  id = "optional",
  categories = Array("All", PluginCategories.recommended),
  label = "Optional",
  description = OptionalAggregator.optionalAggregatorDescription
)
case class OptionalAggregator(@Param(OptionalAggregator.missingValueStrategyDescription)
                              missingValueStrategy: OptionalValueMissingStrategyEnum = OptionalValueMissingStrategyEnum.Ignore) extends Aggregator {
  override def evaluate(weightedValues: Traversable[(Int, Double)]): Option[Double] = {
    require(weightedValues.size == 1, "Optional aggregator only accepts exactly one input!")
    Some(weightedValues.head._2) // TODO: Implement strategies
  }

  override protected def combineIndexes(index1: Index, index2: Index): Index = {
    throw new RuntimeException("Optional aggregator only accepts exactly one input!")
  }
}

object OptionalAggregator {
  final val missingValueStrategyDescription =
"""
The strategy that is applied to missing values, i.e. missing comparison or aggregation results.
Following strategies are possible:

- Ignore: Ignores missing values in subsequent computations, e.g. in an 'And' aggregation the missing value will be ignored.
- True: Evaluate the missing value to the confidence value representing 'True', i.e. 1.0.
- False: Evaluate the missing value to the confidence value representing 'False', i.e. -1.0.
"""

  final val optionalAggregatorDescription =
"""
Handles missing computation values of comparisons or aggregators.
E.g. a comparison produces a missing value if one of its inputs is empty.
It provides several strategies how to cope with missing values: 'Ignore', 'True' and 'False'.
This aggregator must have exactly on input.
"""
}