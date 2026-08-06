package org.silkframework.workbench.workspace

import java.nio.file.Files
import java.time.Duration

import org.silkframework.runtime.plugin.InvalidPluginParameterValueException
import org.silkframework.util.FileUtils._
import org.silkframework.workspace.reports.ExecutionReportManager

class FileExecutionReportManagerTest extends ExecutionReportManagerTest {

  behavior of "FileReportManager"

  it should "fail with a meaningful error if the report directory cannot be created" in {
    val file = Files.createTempFile("Silk_FileReportManagerTest", ".tmp").toFile
    try {
      val ex = intercept[InvalidPluginParameterValueException] {
        FileExecutionReportManager(file.getPath)
      }
      ex.getMessage should include (file.getAbsolutePath)
    } finally {
      file.delete()
    }
  }

  it should "fail with a meaningful error if the report directory is removed after creation" in {
    val tempDir = Files.createTempDirectory("Silk_FileReportManagerTest").toFile
    val reportManager = FileExecutionReportManager(tempDir.getPath)
    tempDir.deleteRecursive()
    val ex = intercept[RuntimeException] {
      reportManager.listReports()
    }
    ex.getMessage should include (tempDir.getAbsolutePath)
  }

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
