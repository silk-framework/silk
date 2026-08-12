package org.silkframework.rule.plugins.distance.tokenbased

import org.silkframework.rule.test.DistanceMeasureTest

class CosineDistanceMetricTest extends DistanceMeasureTest[CosineDistanceMetric] {

  private val metric = CosineDistanceMetric()

  it should "index entities that share a term into the same block, although their weights differ" in {
    val sourceIndex = metric.indexValue("a 0.8;b 0.6", limit = 0.3, sourceOrTarget = true)
    val targetIndex = metric.indexValue("a 0.5;b 0.5", limit = 0.3, sourceOrTarget = false)
    sourceIndex.matches(targetIndex) shouldBe true
  }

  it should "ignore terms with a non-finite score instead of corrupting the similarity" in {
    metric.evaluate("a NaN;b 1.0", "b 1.0", 1.0) shouldBe 0.0
    metric.evaluate("a Infinity", "a 1.0", 1.0) shouldBe 1.0
  }
}
