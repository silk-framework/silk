package org.silkframework.rule.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.entity.Entity
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.util.Identifier

/**
  * A builder for generating transform reports.
  * Not thread safe!
  */
private class TransformReportBuilder(task: Task[TransformSpec], previousReport: TransformReport) {

  private var entityCounter = previousReport.entityCounter

  private var entityErrorCounter = previousReport.entityErrorCounter

  private var ruleResults: Map[Identifier, RuleResult] = {
    previousReport.ruleResults ++ task.data.rules.map(rule => (rule.id, RuleResult())).toMap
  }

  // The maximum number of erroneous values to be held for each rule.
  private val maxSampleErrors = 10

  def addError(rule: TransformRule, entity: Entity, ex: Throwable): Unit = {
    val currentRuleResult = ruleResults(rule.id)

    val updatedRuleResult =
      if(currentRuleResult.sampleErrors.size < maxSampleErrors) {
        val values = rule.sourcePaths.map(p => entity.evaluate(UntypedPath(p.operators)))
        currentRuleResult.withError(RuleError(entity.uri, values, ex))
      } else {
        currentRuleResult.withError()
      }

    ruleResults += ((rule.id, updatedRuleResult))
  }

  def incrementEntityCounter(): Unit = {
    entityCounter += 1
  }

  def incrementEntityErrorCounter(): Unit = {
    entityErrorCounter += 1
  }

  def build(): TransformReport = {
    TransformReport(task, entityCounter, entityErrorCounter, ruleResults, previousReport.globalErrors)
  }
}
