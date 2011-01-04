package de.fuberlin.wiwiss.silk.impl.aggregator

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.aggegrator.GeometricMeanAggregator
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class GeometricMeanAggregatorTest extends FlatSpec with ShouldMatchers
{
    val aggregator = new GeometricMeanAggregator()

    "GeometricMeanAggregator" should "compute the weighted geometric mean" in
    {
        aggregator.evaluate((1, 0.0) :: (2, 0.0) :: (1, 0.0) :: Nil).get should be (approximatelyEqualTo (0.0))
        aggregator.evaluate((1, 1.0) :: (2, 1.0) :: (1, 1.0) :: Nil).get should be (approximatelyEqualTo (1.0))
        aggregator.evaluate((2, 0.5) :: (1, 1.0) :: Nil).get should be (approximatelyEqualTo (0.629961))
        aggregator.evaluate((2, 0.5) :: (1, 1.0) :: (5, 0.7) :: Nil).get should be (approximatelyEqualTo (0.672866))
        aggregator.evaluate((10, 0.1) :: (2, 0.9) :: (3, 0.2) :: Nil).get should be (approximatelyEqualTo (0.153971))
    }
}