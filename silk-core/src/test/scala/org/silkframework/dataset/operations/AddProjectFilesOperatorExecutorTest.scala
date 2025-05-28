package org.silkframework.dataset.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.operations.DeleteFilesOperatorTest.createResourceManager
import org.silkframework.execution.local.LocalExecution
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{ActivityContextMock, MockitoSugar}

class AddProjectFilesOperatorExecutorTest extends AnyFlatSpec with Matchers with TestUserContextTrait with MockitoSugar {

  behavior of "Add project files operator"

  private val executor = LocalAddProjectFilesOperatorExecutor()

  it should "add new files to the project" in {
    execute(
      op = AddProjectFilesOperator("file.csv"),
      newFiles = Seq("fileX.csv", "fileY.csv"),
      existingFiles = Seq("existing.csv"),
      expectedWrittenFiles = Seq("file.csv", "file-1.csv")
    )
  }

  it should "fail if the file already exists and overwrite strategy is set to fail" in {
    val op = AddProjectFilesOperator("file.csv", "", OverwriteStrategyEnum.fail)
    val newFiles = Seq("file.csv")
    val existingFiles = Seq("file.csv")
    val expectedWrittenFiles = Seq.empty[String]

    an[IllegalArgumentException] should be thrownBy {
      execute(op, newFiles, existingFiles, expectedWrittenFiles)
    }
  }

  it should "overwrite existing files if the overwrite strategy is set to overwrite" in {
    val op = AddProjectFilesOperator("file.csv", "", OverwriteStrategyEnum.overwrite)
    val newFiles = Seq("file.csv")
    val existingFiles = Seq("file.csv")
    val expectedWrittenFiles = Seq("file.csv")

    execute(op, newFiles, existingFiles, expectedWrittenFiles)
  }

  it should "ignore existing files if the overwrite strategy is set to ignoreExisting" in {
    val op = AddProjectFilesOperator("file.csv", "", OverwriteStrategyEnum.ignoreExisting)
    val newFiles = Seq("file1.csv", "file2.csv")
    val existingFiles = Seq("file.csv", "other.csv")
    val expectedWrittenFiles = Seq("file-1.csv")

    execute(op, newFiles, existingFiles, expectedWrittenFiles)
  }

  private def execute(op: AddProjectFilesOperator, newFiles: Seq[String], existingFiles: Seq[String], expectedWrittenFiles: Seq[String]): Unit = {
    val task = PlainTask("task", op)
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)
    val activityContext = ActivityContextMock[ExecutionReport]()

    val newFileEntities =
      for(file <- newFiles) yield {
        val fileEntity = FileEntity.createTemp(file)
        fileEntity.file.writeString("new content")
        fileEntity
      }

    executor.execute(task, Seq(FileEntitySchema.create(newFileEntities, task)), output = ExecutorOutput.empty, execution = LocalExecution(), context = activityContext)

    // Test that the files were written
    for(file <- expectedWrittenFiles) {
      val writtenFile = resourceManager.get(file)
      writtenFile shouldBe Symbol("exists")
      writtenFile.loadAsString() shouldBe "new content"
    }

    // Test that the files were not overwritten
    for(file <- existingFiles.toSet -- expectedWrittenFiles) {
      val writtenFile = resourceManager.get(file)
      writtenFile shouldBe Symbol("exists")
      writtenFile.loadAsString() shouldBe "content"
    }
  }
}
