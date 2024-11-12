package org.silkframework.dataset.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}
import org.silkframework.util.{ActivityContextMock, FileUtils, MockitoSugar}

import java.nio.file.Files

class LocalDeleteFilesOperatorExecutorTest extends AnyFlatSpec with Matchers with TestUserContextTrait with MockitoSugar {
  behavior of "Delete files operator"

  private val executor = LocalDeleteFilesOperatorExecutor()

  /** Returns the still existing files after the operator gets executed sorted. */
  private def execute(regex: String, existingFiles: Seq[String]): Seq[String] = {
    val task = PlainTask("task", DeleteFilesOperator(regex))
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)
    val activityContext = ActivityContextMock[ExecutionReport]()
    val outputEntities = executor.execute(task, Seq.empty, output = ExecutorOutput.empty, execution = LocalExecution(), context = activityContext)
    outputEntities mustBe defined
    outputEntities.get.entities.size mustBe (existingFiles.size - resourceManager.listRecursive.size)
    resourceManager.listRecursive.sorted
  }

  private def createResourceManager(files: Seq[String]): ResourceManager = {
    val tempDir = Files.createTempDirectory("test-resource-manager-dir").toFile
    FileUtils.toFileUtils(tempDir).deleteRecursiveOnExit()
    val manager = FileResourceManager(tempDir)
    for(file <- files) {
      manager.get(file).writeString("content")
    }
    manager
  }

  it should "remove files based on a regex" in {
    // Empty regex does nothing
    execute(
      regex = "",
      existingFiles = Seq("file.csv")
    ) mustBe List("file.csv")
    execute(
      regex = "file.*\\.csv",
      existingFiles = Seq("file.csv", "file1.csv", "File1.csv", "files.csv", "subdir/file.csv")
    ) mustBe List("File1.csv", "subdir/file.csv")
  }

  it should "remove files based on a regex in sub-directories" in {
    execute(
      regex = "subdir.*",
      existingFiles = Seq("another", "subdir/file.csv", "subdir/file1.csv", "subdir/File1.csv", "subdir/files.csv", "subdirFile.csv", "this")
    ) mustBe List("another", "this")
    // The regex needs to match the full path
    execute(
      regex = "subdir",
      existingFiles = Seq("another", "subdir/file.csv", "subdir/file1.csv", "subdir/File1.csv", "subdir/files.csv", "subdirFile.csv", "this")
    ) mustBe List("another", "subdir/File1.csv", "subdir/file.csv", "subdir/file1.csv", "subdir/files.csv", "subdirFile.csv", "this")
  }
}
