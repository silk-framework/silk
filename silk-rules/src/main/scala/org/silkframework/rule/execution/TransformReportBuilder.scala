package org.silkframework.rule.execution

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.report.{EntitySample, SampleEntities, SampleEntitiesSchema}
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.util.Identifier

/**
  * A builder for generating transform reports.
  *
  * @param outputTableCount The exact number of output tables expected from this transform execution. Each table must
  *                         call [[outputTableCompleted()]] exactly once. The report is marked done only after all
  *                         registered tables have completed; an incorrect count leaves the report incomplete or
  *                         marks it done too early.
  *
  * Not thread safe!
  */
class TransformReportBuilder(task: Task[TransformSpec],
                             context: ActivityContext[TransformReport],
                             outputTableCount: Int) {

  require(outputTableCount > 0, "A transform report requires at least one output table.")

  private var entityCounter = 0

  private var entityErrorCounter = 0

  private var ruleResults: Map[Identifier, RuleResult] = Map.empty

  private var executionError: Option[String] = None

  private var completedOutputTables = 0

  private var executionDone = false

  private var globalErrors: Seq[String] = Seq.empty

  // The maximum number of erroneous values to be held for each rule.
  private val maxSampleErrors = 10

  private var currentContainerRuleId: String = "root"

  private var currentOutputSampleEntitySchema: SampleEntitiesSchema = SampleEntitiesSchema.empty

  // The samples for the current rule
  private var ruleSampleEntities: Vector[EntitySample] = Vector.empty

  private var sampleOutputEntities: Vector[SampleEntities] = Vector.empty

  private var transformReportContext: Option[TransformReportExecutionContext] = None

  def addRules(rules: Seq[TransformRule]): Unit = {
    ruleResults ++= rules.map(rule => (rule.id, RuleResult())).toMap
  }

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

  /** Sets the 'started' times for the given transform rules.
    *
    * @param coveredRules The IDs of the rules for which the start time should be set
    */
  def setStarted(coveredRules: Set[String]): Unit = {
    ruleResults = ruleResults.map { case (id, results) => (id, if(coveredRules.contains(id)) results.withStarted() else results) }
  }

  /** Sets the 'finished' times for the given transform rules.
    *
    * @param coveredRules The IDs of the rules for which the finish time should be set
    */
  def setFinished(coveredRules: Set[String]): Unit = {
    ruleResults = ruleResults.map { case (id, results) => (id, if(coveredRules.contains(id)) results.withFinished() else results) }
  }

  def setContainerRule(ruleId: String, outputEntitySchema: EntitySchema)
                      (implicit prefixes: Prefixes): Unit = {
    if(ruleId != currentContainerRuleId) {
      ruleSampleEntities = Vector.empty
      currentContainerRuleId = ruleId
    }
    currentOutputSampleEntitySchema = SampleEntitiesSchema.entitySchemaToSampleEntitiesSchema(outputEntitySchema)
  }

  def sampleOutputEntity(entity: => EntitySample): Unit = {
    if(ruleSampleEntities.size < ExecutionReport.SAMPLE_ENTITY_LIMIT) {
      ruleSampleEntities = ruleSampleEntities :+ entity
      sampleOutputEntities = TransformReport.updateOutputSampleEntities(
        SampleEntities(ruleSampleEntities, currentOutputSampleEntitySchema, Some(currentContainerRuleId)),
        sampleOutputEntities
      )
    }
  }

  def addGlobalErrors(errors: Seq[String]): Unit = {
    globalErrors = globalErrors ++ errors
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

  /** Records completion of one output table and marks the report done after the final table completes. */
  def outputTableCompleted(): Unit = {
    if(!executionDone) {
      completedOutputTables += 1
      require(
        completedOutputTables <= outputTableCount,
        "More output tables completed than were registered for this transform execution."
      )
      executionDone = completedOutputTables == outputTableCount
      build(isDone = executionDone)
    }
  }

  /** Records a terminal transform failure. Subsequent table completions do not overwrite the failed report. */
  def executionFailed(error: String): Unit = {
    if(!executionDone) {
      setExecutionError(error)
      executionDone = true
      build(isDone = true)
    }
  }

  def setExecutionContext(context: TransformReportExecutionContext): Unit = {
    transformReportContext = Some(context)
  }

  def build(isDone: Boolean = false, logMessage: Boolean = false): Unit = {
    context.value() = TransformReport(task, entityCounter, entityErrorCounter, ruleResults, globalErrors, isDone, executionError,
      sampleOutputEntities = sampleOutputEntities, context = transformReportContext)
    if(logMessage) {
      context.status.updateMessage(s"Executing ($entityCounter Entities)")
    }
  }
}
