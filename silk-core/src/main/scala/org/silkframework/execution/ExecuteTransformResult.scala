package org.silkframework.execution

import org.silkframework.execution.ExecuteTransformResult._

/**
  * Holds the state of the transform execution.
  *
  * @param entityCounter The number of entities that have been transformed.
  * @param entityErrorCounter The number of entities that have been erroneous.
  * @param ruleResults The transformation statistics for each mapping rule by name.
  */
case class ExecuteTransformResult(entityCounter: Long = 0L, entityErrorCounter: Long = 0L,
                                  ruleResults: Map[String, RuleResult] = Map.empty) extends ExecutionReport {

  def withError(ruleName: String, ruleError: RuleError) = {
    val updatedRuleResult = ruleResults.getOrElse(ruleName, RuleResult()).withError(ruleError)
    copy(ruleResults = ruleResults + ((ruleName, updatedRuleResult)))
  }

}

object ExecuteTransformResult {

  val maxSampleErrors = 10

  /**
    * The transformation statistics for a single mapping rule.
    *
    * @param errorCount The number of (validation) errors for this rule.
    * @param sampleErrors Samples of erroneous values. This is just an excerpt. If all erroneous values are needed,
    *                     the transform executor needs to be configured with an error output.
    */
  case class RuleResult(errorCount: Long = 0L, sampleErrors: IndexedSeq[RuleError] = IndexedSeq.empty) {

    def withError(error: RuleError) = {
      copy(
        errorCount = errorCount + 1,
        sampleErrors =
          if(sampleErrors.size < maxSampleErrors)
            sampleErrors :+ error
          else
            sampleErrors
      )
    }

  }

  /**
    * A single transformation error.
    *
    * @param value The erroneous value
    * @param exception The cause
    */
  case class RuleError(value: Seq[Seq[String]], exception: Exception)

}
