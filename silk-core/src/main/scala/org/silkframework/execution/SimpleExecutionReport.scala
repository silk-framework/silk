package org.silkframework.execution

/**
  * An execution report that only contains generic task-independent execution information.
  */
case class SimpleExecutionReport(label: String,
                                 summary: Seq[(String, String)],
                                 warning: Option[String]) extends ExecutionReport
