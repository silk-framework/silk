package org.silkframework.rule.execution

import org.silkframework.config.Task
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.TransformReport._
import org.silkframework.util.Identifier

/**
  * Holds the state of the transform execution.
  *
  * @param entityCount The number of entities that have been transformed, including erroneous entities.
  * @param entityErrorCount The number of entities that have been erroneous.
  * @param ruleResults The transformation statistics for each mapping rule by name.
  */
case class TransformReport(task: Task[TransformSpec],
                           entityCount: Int = 0,
                           entityErrorCount: Int = 0,
                           ruleResults: Map[Identifier, RuleResult] = Map.empty,
                           globalErrors: Seq[String] = Seq.empty,
                           isDone: Boolean = false,
                           override val error: Option[String] = None) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "Number of entities" -> entityCount.toString,
      "Entities with issues" -> entityErrorCount.toString
    )
  }

  def warnings: Seq[String] = {
    var allErrors = globalErrors
    if(entityErrorCount != 0) {
      allErrors :+= s"Validation issues occurred on $entityErrorCount entities."
    }
    allErrors
  }

  /**
    * Short description of the operation (plural, past tense).
    */
  override def operationDesc: String = "entities generated"

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport = copy(isDone = true)
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
    def withError(): RuleResult = {
      copy(
        errorCount = errorCount + 1
      )
    }

    /**
      * Increases the error counter and adds a new sample error.
      */
    def withError(error: RuleError): RuleResult = {
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
    * @param message The error description
    */
  case class RuleError(entity: String, value: Seq[Seq[String]], message: String, operatorId: Option[Identifier] = None)

  object RuleError {
    def fromException(entity: String, value: Seq[Seq[String]], exception: Throwable, operatorId: Option[Identifier] = None): RuleError = {
      new RuleError(entity, value, exception.getMessage, operatorId)
    }
  }

}
