package org.silkframework.rule.execution

import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.execution.TransformReport._
import org.silkframework.rule.TransformRule
import org.silkframework.util.Identifier

/**
  * Holds the state of the transform execution.
  *
  * @param entityCounter The number of entities that have been transformed, including erroneous entities.
  * @param entityErrorCounter The number of entities that have been erroneous.
  * @param ruleResults The transformation statistics for each mapping rule by name.
  */
case class TransformReport(
                            entityCounter: Long = 0L,
                            entityErrorCounter: Long = 0L,
                            ruleResults: Map[Identifier, RuleResult] = Map.empty
                          ) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "number of entities" -> entityCounter.toString,
      "number of errors" -> entityErrorCounter.toString
    )
  }

}

object TransformReport {

  /**
    * The transformation statistics for a single mapping rule.
    *
    * @param errorCount The number of (validation) errors for this rule.
    * @param sampleErrors Samples of erroneous values. This is just an excerpt. If all erroneous values are needed,
    *                     the transform executor needs to be configured with an error output.
    */
  case class RuleResult(errorCount: Long = 0L, sampleErrors: IndexedSeq[RuleError] = IndexedSeq.empty) {

    /**
      * Increases the error counter, but does not add a new sample error.
      */
    def withError() = {
      copy(
        errorCount = errorCount + 1
      )
    }

    /**
      * Increases the error counter and adds a new sample error.
      */
    def withError(error: RuleError) = {
      copy(
        errorCount = errorCount + 1,
        sampleErrors :+ error
      )
    }

  }

  /**
    * A single transformation error.
    *
    * @param entity The URI of the entity for which the error occurred.
    * @param value The erroneous value
    * @param exception The cause
    */
  case class RuleError(entity: String, value: Seq[Seq[String]], exception: Throwable)

}
