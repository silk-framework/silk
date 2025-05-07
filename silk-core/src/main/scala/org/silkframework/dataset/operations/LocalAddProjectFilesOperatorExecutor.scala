package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema, TypedEntities}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager

/** Executes the delete files operator. */
case class LocalAddProjectFilesOperatorExecutor() extends LocalExecutor[AddProjectFilesOperator] {

  override def execute(task: Task[AddProjectFilesOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {

    assert(inputs.size == 1, "Add project files operator expects exactly one input")
    val input = inputs.head
    input match {
      case FileEntitySchema(files) =>
        addFiles(files, pluginContext.resources, task, context)
      case _ =>
        throw new IllegalArgumentException("Input must be of type FileEntitySchema. Got: " + input.entitySchema)
    }
    None
  }

  private def addFiles(files: TypedEntities[FileEntity, _], targetDirectory: ResourceManager,
                       task: Task[AddProjectFilesOperator], context: ActivityContext[ExecutionReport]): Unit = {
    val executionReport = AddProjectFilesOperatorExecutionReportUpdater(task, context)
    val fileNameGenerator = new FileNameGenerator(task.data.fileName)
    for (fileEntity <- files.typedEntities) {
      val file = fileEntity.file
      val newFileName = fileNameGenerator(file.name)
      if (task.data.overwriteStrategy == OverwriteStrategyEnum.overwrite || !targetDirectory.exists(newFileName)) {
        targetDirectory.get(newFileName).writeResource(file)
        executionReport.addFile(file.name)
        executionReport.increaseEntityCounter()
      } else {
        throw new IllegalArgumentException(s"Project file already exists: ${file.name}")
      }
    }
    executionReport.executionDone()
  }

  /**
   * Generates new file names.
   *
   * @param newFileName File name of the uploaded file(s). If multiple files are uploaded, an index will be appended to the file name.
   *                    If left empty, the existing file names will be used.
   */
  private class FileNameGenerator(newFileName: String) {
    private var index = 0

    private val dotIndex = newFileName.lastIndexOf('.')
    private val baseName = if (dotIndex > 0) newFileName.substring(0, dotIndex) else newFileName
    private val extension = if (dotIndex > 0) newFileName.substring(dotIndex) else ""

    def apply(existingFileName: String): String = {
      if (newFileName.isEmpty) {
        existingFileName
      } else {
        val fileName = baseName + (if (index > 0) s"-$index" else "") + extension
        index += 1
        fileName
      }
    }
  }
}

case class AddProjectFilesOperatorExecutionReportUpdater(task: Task[TaskSpec],
                                                         context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
  private var files = Vector.empty[String]

  override def operationLabel: Option[String] = Some("add files")

  override def entityLabelSingle: String = "file"

  override def entityLabelPlural: String = "files"

  override def entityProcessVerb: String = "added"

  override def minEntitiesBetweenUpdates: Int = 1

  def addFile(file: String): Unit = {
    files = files appended file
  }

  override def additionalFields(): Seq[(String, String)] = {
    // files is still null when this method is called in the constructor of ExecutionReportUpdater
    val addedFiles = if(files != null) files.mkString(", ") else ""
    Seq(
      "Added files" -> addedFiles
    )
  }
}