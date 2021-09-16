package org.silkframework.execution

import org.silkframework.config.{Task, TaskSpec}

/**
  * An execution report that only contains generic task-independent execution information.
  */
case class SimpleExecutionReport(task: Task[TaskSpec],
                                 summary: Seq[(String, String)],
                                 warnings: Seq[String],
                                 isDone: Boolean,
                                 entityCount: Int,
                                 override val operation: Option[String] = None) extends ExecutionReport