package org.silkframework.rule.execution

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{Path, Restriction}
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.aggegrator.{MaximumAggregator, MinimumAggregator, NegationAggregator}
import org.silkframework.rule.plugins.distance.equality.{EqualityMetric, InequalityMetric}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.{Aggregation, Comparison, DistanceMeasure, SimilarityOperator}
import org.silkframework.rule.{BooleanLinkageRule, LinkageRule}
import org.silkframework.util.DPair

class ComparisonToRestrictionConverterTest extends FlatSpec with MustMatchers {
  behavior of "Comparison to Restriction Converter"

  private val comparisonToRestrictionConverter = new ComparisonToRestrictionConverter()

  it should "convert a linkage rule of a simple and" in {
    val notSatisfiable = and(
      sourceEqual("http://p1", "p1"),
      targetEqual("http://t1", "t1")
    )
    convert(notSatisfiable, sourceOrTarget = true).get.serialize mustBe
      """?a <http://p1> ?var_pref_0_0_Value .
        |
        |FILTER (((STR(?var_pref_0_0_Value) = "p1")))""".stripMargin
    convert(notSatisfiable, sourceOrTarget = false).get.serialize mustBe
      """?a <http://t1> ?var_pref_1_0_Value .
        |
        |FILTER (((STR(?var_pref_1_0_Value) = "t1")))""".stripMargin
  }

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
    convert(andOrMix, sourceOrTarget = true, removeInequalities = false).get.serialize mustBe
        """?a <http://P1> ?var_pref_0_0_Value .
          |?a <http://P3> ?var_pref_0_1_Value .
          |?a <http://P2> ?var_pref_1_0_Value .
          |
          |FILTER (((STR(?var_pref_0_0_Value) != "P1" || STR(?var_pref_0_1_Value) != "P3") && (STR(?var_pref_1_0_Value) = "P2")))""".stripMargin
    convert(andOrMix, sourceOrTarget = false, removeInequalities = false).get.serialize mustBe
        """?a <http://T1> ?var_pref_2_0_Value .
          |?a <http://T2> ?var_pref_2_1_Value .
          |
          |FILTER (((STR(?var_pref_2_0_Value) = "T1" || STR(?var_pref_2_1_Value) != "T2")))""".stripMargin
    // Inequalities should be removed by default
    convert(andOrMix, sourceOrTarget = true).get.serialize mustBe
        """?a <http://P2> ?var_pref_1_0_Value .
          |
          |FILTER (((STR(?var_pref_1_0_Value) = "P2")))""".stripMargin
    // There will be nothing left for andOrMix when all inequalities are filtered out
    convert(andOrMix, sourceOrTarget = false) mustBe empty
  }

  it should "not convert a linkage rule that cannot be fully satisfied by a filter" in {
    val notSatisfiable = and(
      or( /** Mixing source restriction and target restriction in the same disjunction. This cannot be pushed as a filter
              for neither the source nor the target data source. */
        sourceEqual("http://p1", "p1"),
        targetEqual("http://t1", "t1")
      )
    )
    convert(notSatisfiable, sourceOrTarget = true) mustBe None
    convert(notSatisfiable, sourceOrTarget = false) mustBe None
  }

  it should "fail early for exponentially exploding conversions" in {
    /*
    This kind of source clauses will lead to exponential explosion when converted into a CNF:
    (X1 ^ Y1) v (X2 ^ Y2) v ... v (Xn ^ Yn)
    The algorithm should have some way to prevent running out of memory and fail early.
     */
    val maxComparisons = BooleanLinkageRule.MAX_COMPARISONS_IN_LINKAGE_RULE_FOR_CNF_CONVERSION
    val okRule = exponentiallyExplodingRule(maxComparisons)
    val notOkRule = exponentiallyExplodingRule(maxComparisons + 2) // + 2 because this is divided by 2 later
    convert(okRule, sourceOrTarget = true) mustBe defined
    convert(notOkRule, sourceOrTarget = true).isEmpty mustBe true
  }

  private def exponentiallyExplodingRule(nrOfComparisons: Int): Aggregation = {
    or(
      (for(i <- 1 to (nrOfComparisons / 2)) yield {
        andPair(i.toString)
      }) :_*
    )
  }

  private def andPair(suffix: String): Aggregation = {
    and(
      sourceEqual("A" + suffix, "A" + suffix),
      sourceEqual("B" + suffix, "B" + suffix)
    )
  }

  private def convert(operator: SimilarityOperator,
                      subject: String = "a",
                      variablePrefix: String = "var_pref_",
                      sourceOrTarget: Boolean,
                      removeInequalities: Boolean = true): Option[Restriction.Operator] = {
    comparisonToRestrictionConverter.linkageRuleToRestriction(
      LinkageRule(Some(operator)), subject, variablePrefix, sourceOrTarget, removeInequalities = removeInequalities)
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
