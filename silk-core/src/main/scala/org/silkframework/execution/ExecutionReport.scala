package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.report.EntitySample

/**
  * Contains statistics about the execution of a task.
  */
trait ExecutionReport {

  /**
    * The task that corresponds to this report.
    */
  def task: Task[TaskSpec]

  /**
    * A user-friendly label for this report, usually just the task label.
    */
  def label: String = task.label()

  /**
    * Short label for the executed operation, e.g., read or write (optional).
    */
  def operation: Option[String] = None

  /**
    * Short description of the operation (plural, past tense).
    * Example: "entities processed".
    */
  def operationDesc: String

  /**
    * Generates a short summary of this report.
    *
    * @return A sequence of key-value pairs representing the summary table.
    */
  def summary: Seq[(String, String)]

  /**
    * If issues occurred during execution, this contains a list of user-friendly messages.
    */
  def warnings: Seq[String]

  /**
    * Error message in case a fatal error occurred.
    */
  def error: Option[String] = None

  /**
    * True, if the execution finished.
    * False, if the execution is still running and the report reflects the current execution progress.
    */
  def isDone: Boolean

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport

  /**
    * The number of entities that have been processed.
    */
  def entityCount: Int

  /** Sample of entities that were output by this task. */
  def sampleOutputEntities: Seq[EntitySample]

  /** Updates the execution report with some sample entities. */
  def withSampleOutputEntities(entities: Seq[EntitySample]): Unit
}

object ExecutionReport {

  final val DEFAULT_OPERATION_DESC = "entities processed"

  final val SAMPLE_ENTITY_LIMIT = 5

}
