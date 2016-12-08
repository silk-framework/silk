package org.silkframework.rule.execution

import org.silkframework.entity.Entity
import org.silkframework.rule.TransformRule
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.util.Identifier

/**
  * A builder for generating transform reports.
  * Not thread safe!
  */
private class TransformReportBuilder(rules: Seq[TransformRule]) {

  private var entityCounter = 0L

  private var entityErrorCounter = 0L

  private var ruleResults: Map[Identifier, RuleResult] = rules.map(rule => (rule.name, RuleResult())).toMap

  // The maximum number of erroneous values to be held for each rule.
  private val maxSampleErrors = 10

  def addError(rule: TransformRule, entity: Entity, ex: Throwable): Unit = {
    val currentRuleResult = ruleResults(rule.name)

    val updatedRuleResult =
      if(currentRuleResult.sampleErrors.size < maxSampleErrors) {
        val values = rule.paths.map(entity.evaluate)
        currentRuleResult.withError(RuleError(entity.uri, values, ex))
      } else {
        currentRuleResult.withError()
      }

    ruleResults += ((rule.name, updatedRuleResult))
  }

  def incrementEntityCounter(): Unit = {
    entityCounter += 1
  }

  def incrementEntityErrorCounter(): Unit = {
    entityErrorCounter += 1
  }

  def build(): TransformReport = {
    TransformReport(entityCounter, entityErrorCounter, ruleResults)
  }

}