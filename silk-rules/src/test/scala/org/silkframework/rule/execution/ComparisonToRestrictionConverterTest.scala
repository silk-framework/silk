package org.silkframework.rule.execution

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{Path, Restriction}
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.aggegrator.{MaximumAggregator, MinimumAggregator, NegationAggregator}
import org.silkframework.rule.plugins.distance.equality.{EqualityMetric, InequalityMetric}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.{Aggregation, Comparison, DistanceMeasure, SimilarityOperator}
import org.silkframework.util.DPair

class ComparisonToRestrictionConverterTest extends FlatSpec with MustMatchers {
  behavior of "Comparison to Restriction Converter"

  it should "convert a boolean linkage rule into a SPARQL filter" in {
    val andOrMix = and(
      or(
        not(sourceEqual("http://P1", "P1")),
        sourceInEqual("http://P3", "P3")
      ),
      or(
        sourceEqual("http://P2", "P2")
      ),
      or(
        targetEqual("http://T1", "T1"),
        not(targetInEqual("http://T2", "T2"))
      )
    )
    convert(andOrMix, sourceOrTarget = true).serialize mustBe
        """?a <http://P1> ?var_pref_0_0_Value .
          |?a <http://P3> ?var_pref_0_1_Value .
          |?a <http://P2> ?var_pref_1_0_Value .
          |
          |FILTER (((STR(?var_pref_0_0_Value) != "P1" || STR(?var_pref_0_1_Value) != "P3") && (STR(?var_pref_1_0_Value) = "P2")))""".stripMargin
    convert(andOrMix, sourceOrTarget = false).serialize mustBe
        """?a <http://T1> ?var_pref_2_0_Value .
          |?a <http://T2> ?var_pref_2_1_Value .
          |
          |FILTER (((STR(?var_pref_2_0_Value) = "T1" || STR(?var_pref_2_1_Value) != "T2")))""".stripMargin
  }

  private def convert(operator: SimilarityOperator, subject: String = "a", variablePrefix: String = "var_pref_", sourceOrTarget: Boolean): Restriction.Operator = {
    ComparisonToRestrictionConverter.linkageRuleToSparqlFilter(
      LinkageRule(Some(operator)), subject, variablePrefix, sourceOrTarget).get
  }

  private def and(operators: SimilarityOperator*): Aggregation = Aggregation(aggregator = MinimumAggregator(), operators = operators)
  private def or(operators: SimilarityOperator*): Aggregation = Aggregation(aggregator = MaximumAggregator(), operators = operators)
  private def not(operator: SimilarityOperator): Aggregation = Aggregation(aggregator = NegationAggregator(), operators = Seq(operator))
  private def sourceEqual(sourcePath: String, constant: String): Comparison = {
    sourceComparison(sourcePath, constant, EqualityMetric())
  }
  private def targetEqual(targetPath: String, constant: String): Comparison = {
    targetComparison(targetPath, constant, EqualityMetric())
  }
  private def sourceInEqual(sourcePath: String, constant: String): Comparison = {
    sourceComparison(sourcePath, constant, InequalityMetric())
  }
  private def targetInEqual(targetPath: String, constant: String): Comparison = {
    targetComparison(targetPath, constant, EqualityMetric())
  }
  private def sourceComparison(sourcePath: String, constant: String, metric: DistanceMeasure): Comparison = Comparison(
    metric = metric,
    inputs = DPair(
      PathInput(path = Path(sourcePath)),
      TransformInput(transformer = ConstantTransformer(constant))
    )
  )
  private def targetComparison(targetPath: String, constant: String, metric: DistanceMeasure): Comparison = {
    val comparison = sourceComparison(targetPath, constant, metric)
    comparison.copy(inputs = comparison.inputs.swap)
  }
}
