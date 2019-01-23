package de.fuberlin.wiwiss.silk.plugins.spatial.relation

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.plugins.spatial.relation.NegateMetric

class NegateMetricTest extends FlatSpec with Matchers {

  it should "return INF on negated disjoint" in {
    NegateMetric(relation="FF*FF****")
      .evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POINT (3 3)", 0.0) shouldEqual Double.PositiveInfinity
  }

  it should "return 0.0 on negated contains" in {
    NegateMetric(relation="contains")
      .evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POINT (0 3)", 0.0) shouldEqual 0.0
  }

  it should "return 0.0 on negated overlaps" in {
    NegateMetric(relation="overlaps")
      .evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POLYGON ((3 0, 0 3, 0 0))", 0.0) shouldEqual 0.0
  }

  it should "throw exception on double negation" in {
    intercept[IllegalArgumentException](
      NegateMetric(blockingParameter = 1.0, metric = NegateMetric(relation="contains"))
        .evaluate("POLYGON ((0 0, 0 2, 2 2, 2 0, 0 0))", "POINT (0 2)", 0.0) shouldEqual Double.PositiveInfinity
    )
  }
}
