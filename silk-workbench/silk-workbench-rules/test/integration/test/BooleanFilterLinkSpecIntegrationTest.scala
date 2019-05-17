package integration.test

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.execution.ComparisonToRestrictionConverter
import org.silkframework.util.{ConfigTestTrait, DPair}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity

/**
  * Tests the boolean filtering optimization of link specs.
  * If a linkage rule can be converted into a boolean [[org.silkframework.rule.BooleanLinkageRule]], specific
  * rule patterns can be turned into filters that can be applied on the data source.
  */
class BooleanFilterLinkSpecIntegrationTest extends FlatSpec
    with SingleProjectWorkspaceProviderTestTrait
    with MustMatchers
    with ConfigTestTrait{
  override def projectPathInClasspath: String = "diProjects/booleanFilterProject.zip"

  lazy private val comparisonToRestrictionConverter = new ComparisonToRestrictionConverter()

  case class ExpectedStats(sourceEntities: Int, targetEntities: Int, nrLinks: Int)

  private val testCases = Seq(
    "AndWithFilterAndPathComparison" -> ExpectedStats(4, 2, 2),
    "AndFilterIntersect" -> ExpectedStats(4, 1, 4), // filter with selectivity of 2 intersects filter with selectivity of 2 with one common entity => 1 entity
    "OrFilterUnion" -> ExpectedStats(4, 3, 12), // filter with selectivity of 2 combined with other filter with selectivity of 2 that share one common entity => 3 entities
    "OrFilterUnionWithAndPathComparison" -> ExpectedStats(4, 3, 12), // with added path comparison as child of CNF AND-clause this has no effect on filters from other AND children => same filters
    "OrFilterUnionWithPathComparison" -> ExpectedStats(4, 4, 16), // with added path comparison in CNF OR-clause the filters cannot be pushed into the data source => no filtering
    "ComplexBooleanExpression1" -> ExpectedStats(4, 1, 4), // pure target entity filter that should leave entity :e3
    "OrWithFilterFromBothSources" -> ExpectedStats(4, 4, 12), // Filters for both sources in the same CNF OR-clause will no be pushed into data source => no filtering
    "ComplexBooleanExpression2" -> ExpectedStats(3, 3, 9),
    "numericLinkingExact" -> ExpectedStats(1, 1, 1),
    "numericLinkingLoose" -> ExpectedStats(2, 2, 4)
  )

  it should "disable pushing inequality filters when configured so" in {
    comparisonToRestrictionConverter.removeInequalityClauses mustBe false
  }

  override def workspaceProvider: String = "inMemory"

  for((testCase, ExpectedStats(expectedSourceEntities, expectedTargetEntities, expectedNrLinks)) <- testCases) {
    it should s"filter the correct number of entities for test case $testCase" in {
      val linkingTask = project.task[LinkSpec](testCase)
      val evaluateLinkingActivity = linkingTask.activity[EvaluateLinkingActivity]
      evaluateLinkingActivity.control.startBlocking()
      val linkingResult = evaluateLinkingActivity.value
      val DPair(sourceEntities, targetEntities) = linkingResult.statistics.entityCount
      val nrLinks = linkingResult.links.size
      sourceEntities mustBe expectedSourceEntities
      targetEntities mustBe expectedTargetEntities
      nrLinks mustBe expectedNrLinks
    }
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "optimizations.linking.execution.pushFilters.removeDisjunctionsWithInEqualities" -> Some("false")
  )
}
