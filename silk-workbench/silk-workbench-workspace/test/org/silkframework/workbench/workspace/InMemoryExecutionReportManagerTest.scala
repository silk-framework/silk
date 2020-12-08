package org.silkframework.workbench.workspace

import java.time.Duration

import org.silkframework.workspace.reports.{ExecutionReportManager, InMemoryExecutionReportManager}

class InMemoryExecutionReportManagerTest extends ExecutionReportManagerTest {

  behavior of "InMemoryExecutionReportManager"

  override protected def withReportManager(retentionTime: Duration = ExecutionReportManager.DEFAULT_RETENTION_TIME)(f: ExecutionReportManager => Unit): Unit = {
    InMemoryExecutionReportManager(retentionTime)
  }

}
