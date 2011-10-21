package de.fuberlin.wiwiss.silk.plugins.aggregator

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.aggegrator.QuadraticMeanAggregator
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class QuadraticMeanAggregatorTest extends FlatSpec with ShouldMatchers {
  val aggregator = new QuadraticMeanAggregator()

  "QuadraticMeanAggregator" should "compute the weighted quadratic mean" in {
    aggregator.evaluate((1, 1.0) :: (1, 1.0) :: (1, 1.0) :: Nil).get should be(approximatelyEqualTo(1.0))
    aggregator.evaluate((1, 1.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.707107))
    aggregator.evaluate((1, 0.4) :: (1, 0.5) :: (1, 0.6) :: Nil).get should be(approximatelyEqualTo(0.506623))
    aggregator.evaluate((1, 0.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.0))
    aggregator.evaluate((2, 1.0) :: (1, 0.0) :: (1, 0.0) :: Nil).get should be(approximatelyEqualTo(0.707107))
    aggregator.evaluate((1, 0.4) :: (2, 0.5) :: (3, 0.6) :: Nil).get should be(approximatelyEqualTo(0.538516))
  }
}