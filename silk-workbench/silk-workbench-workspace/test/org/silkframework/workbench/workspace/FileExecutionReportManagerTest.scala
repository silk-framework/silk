package org.silkframework.workbench.workspace

import java.nio.file.Files
import java.time.Duration

import org.silkframework.util.FileUtils._
import org.silkframework.workspace.reports.ExecutionReportManager

class FileExecutionReportManagerTest extends ExecutionReportManagerTest {

  behavior of "FileReportManager"

  override protected def withReportManager(retentionTime: Duration = ExecutionReportManager.DEFAULT_RETENTION_TIME)(f: ExecutionReportManager => Unit): Unit = {
    val tempDir = Files.createTempDirectory("Silk_FileReportManagerTest").toFile
    try {
      val reportManager = FileExecutionReportManager(tempDir.getPath, retentionTime)
      f(reportManager)
    } finally {
      tempDir.deleteRecursive()
    }
  }

}
