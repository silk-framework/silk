package de.fuberlin.wiwiss.silk.impl.aggregator

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.aggegrator.AverageAggregator
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class AverageAggregatorTest extends FlatSpec with ShouldMatchers {
  "AverageAggregator" should "compute the arithmetic mean for non-weighted inputs" in {
    val aggregator = new AverageAggregator()

    aggregator.evaluate((1, 1.0) :: (1, 1.0) :: (1, 1.0) :: Nil).get should be(approximatelyEqualTo(1.0))
    aggregator.evaluate((1, 1.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.5))
    aggregator.evaluate((1, 0.4) :: (1, 0.5) :: (1, 0.6) :: Nil).get should be(approximatelyEqualTo(0.5))
    aggregator.evaluate((1, 0.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.0))
  }

  "AverageAggregator" should "compute the weighted arithmetic mean for weighted inputs" in {
    val aggregator = new AverageAggregator()

    aggregator.evaluate((2, 1.0) :: (1, 0.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.5))
  }
}