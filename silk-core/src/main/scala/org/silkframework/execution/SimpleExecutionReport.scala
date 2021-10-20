package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}

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
                                 override val operationDesc: String = ExecutionReport.DEFAULT_OPERATION_DESC) extends ExecutionReport {

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport = copy(isDone = true)

}