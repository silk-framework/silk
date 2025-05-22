package org.silkframework.dataset.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.operations.DeleteFilesOperatorTest.createResourceManager
import org.silkframework.execution.local.LocalExecution
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{ActivityContextMock, MockitoSugar}

class LocalDeleteFilesOperatorExecutorTest extends AnyFlatSpec with Matchers with TestUserContextTrait with MockitoSugar {

  behavior of "Delete files operator"

  private val executor = LocalDeleteFilesOperatorExecutor()

  it should "remove files based on a regex" in {
    // Empty regex does nothing
    execute(
      regex = "",
      existingFiles = Seq("file.csv"),
      entityOutput = false
    ) shouldBe List("file.csv")
    execute(
      regex = "file.*\\.csv",
      existingFiles = Seq("file.csv", "file1.csv", "File1.csv", "files.csv", "subdir/file.csv"),
      entityOutput = true
    ) shouldBe List("File1.csv", "subdir/file.csv")
  }

  it should "remove files based on a regex in sub-directories" in {
    execute(
      regex = "subdir.*",
      existingFiles = Seq("another", "subdir/file.csv", "subdir/file1.csv", "subdir/File1.csv", "subdir/files.csv", "subdirFile.csv", "this"),
      entityOutput = true
    ) shouldBe List("another", "this")
    // The regex needs to match the full path
    execute(
      regex = "subdir",
      existingFiles = Seq("another", "subdir/file.csv", "subdir/file1.csv", "subdir/File1.csv", "subdir/files.csv", "subdirFile.csv", "this"),
      entityOutput = true
    ) shouldBe List("another", "subdir/File1.csv", "subdir/file.csv", "subdir/file1.csv", "subdir/files.csv", "subdirFile.csv", "this")
  }

  /** Returns the still existing files after the operator gets executed sorted. */
  private def execute(regex: String, existingFiles: Seq[String], entityOutput: Boolean): Seq[String] = {
    val task = PlainTask("task", DeleteFilesOperator(regex, outputEntities = entityOutput))
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)
    val activityContext = ActivityContextMock[ExecutionReport]()
    val outputEntities = executor.execute(task, Seq.empty, output = ExecutorOutput.empty, execution = LocalExecution(), context = activityContext)
    if(entityOutput) {
      outputEntities shouldBe defined
      outputEntities.get.entities.size shouldBe (existingFiles.size - resourceManager.listRecursive.size)
    } else {
      outputEntities should not be defined
    }
    resourceManager.listRecursive.sorted
  }
}
