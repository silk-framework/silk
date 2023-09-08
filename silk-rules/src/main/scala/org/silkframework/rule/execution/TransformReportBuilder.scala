package org.silkframework.rule.execution

import org.silkframework.config.Task
import org.silkframework.entity.Entity
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.util.Identifier

/**
  * A builder for generating transform reports.
  * Not thread safe!
  */
private class TransformReportBuilder(task: Task[TransformSpec], rules: Seq[TransformRule],  previousReport: TransformReport) {

  private var entityCounter = previousReport.entityCount

  private var entityErrorCounter = previousReport.entityErrorCount

  private var ruleResults: Map[Identifier, RuleResult] = {
    previousReport.ruleResults ++ rules.map(rule => (rule.id, RuleResult())).toMap
  }

  private var executionError: Option[String] = None

  // The maximum number of erroneous values to be held for each rule.
  private val maxSampleErrors = 10

  def addRuleError(rule: TransformRule, entity: Entity, ex: Throwable, operatorId: Option[Identifier] = None): Unit = {
    val currentRuleResult = ruleResults(rule.id)

    val updatedRuleResult =
      if(currentRuleResult.sampleErrors.size < maxSampleErrors) {
        val values = rule.sourcePaths.map(p => entity.evaluate(UntypedPath(p.operators)))
        currentRuleResult.withError(RuleError.fromException(entity.uri, values, ex, operatorId))
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

  def setExecutionError(error: String): Unit = {
    executionError = Some(error)
  }

  def build(isDone: Boolean = false): TransformReport = {
    TransformReport(task, entityCounter, entityErrorCounter, ruleResults, previousReport.globalErrors, isDone, executionError)
  }
}
