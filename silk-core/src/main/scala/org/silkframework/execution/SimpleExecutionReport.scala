package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.report.SampleEntities

/**
  * An execution report that only contains generic task-independent execution information.
  */
case class SimpleExecutionReport(task: Task[TaskSpec],
                                 summary: Seq[(String, String)],
                                 warnings: Seq[String],
                                 override val error: Option[String],
                                 isDone: Boolean,
                                 entityCount: Int,
                                 override val operation: Option[String] = None,
                                 override val operationDesc: String = ExecutionReport.DEFAULT_OPERATION_DESC,
                                 override val sampleOutputEntities: Seq[SampleEntities] = Seq.empty) extends ExecutionReport {

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport = copy(isDone = true)

  /**
    * Checks if this is still the initial empty report.
    */
  def isEmpty: Boolean = {
    summary.isEmpty && warnings.isEmpty && error.isEmpty && !isDone && entityCount == 0 && operation.isEmpty
  }

  /** Updates the execution report with some sample entities. */
  override def withSampleOutputEntities(sampleEntities: SampleEntities): ExecutionReport = this.copy(sampleOutputEntities = Seq(sampleEntities))
}

object SimpleExecutionReport {

  def initial(task: Task[TaskSpec]): SimpleExecutionReport = {
    SimpleExecutionReport(task, Seq.empty, Seq.empty, None, isDone = false, entityCount = 0)
  }
}