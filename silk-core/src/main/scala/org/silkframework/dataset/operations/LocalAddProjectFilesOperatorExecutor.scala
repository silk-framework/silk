package org.silkframework.dataset.operations

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema, TypedEntities}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager

import scala.collection.mutable

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
        val executionReport = AddProjectFilesOperatorExecutionReportUpdater(task, context)
        new FileWriter(task.data.getDirectory(pluginContext.resources), task.data, executionReport).write(files)
      case _ =>
        throw new IllegalArgumentException("Input must be of type FileEntitySchema. Got: " + input.entitySchema)
    }
    None
  }

  private class FileWriter(targetDirectory: ResourceManager,
                           operator: AddProjectFilesOperator,
                           executionReport: AddProjectFilesOperatorExecutionReportUpdater) {

    private val fileNameGenerator = new FileNameGenerator(operator.fileName)
    private val warningFiles = mutable.Buffer[String]()

    def write(files: TypedEntities[FileEntity, _]): Unit = {
      for (fileEntity <- files.typedEntities) {
        val file = fileEntity.file
        val newFileName = fileNameGenerator(file.name)
        val fileExists = targetDirectory.exists(newFileName)
        if (addFile(newFileName, fileExists)) {
          targetDirectory.get(newFileName).writeResource(file)
          executionReport.increaseEntityCounter()
        }
      }

      if(warningFiles.nonEmpty) {
        executionReport.addWarning(s"The following file(s) already existed and were overwritten: ${warningFiles.mkString(", ")}")
      }
      executionReport.executionDone()
    }

    private def addFile(fileName: String, fileExists: Boolean): Boolean = {
      operator.overwriteStrategy match {
        case OverwriteStrategyEnum.overwrite =>
          // Always write the file
          if (fileExists) {
            executionReport.addOverwrittenFile(operator.targetDirectory + fileName)
          } else {
            executionReport.addNewFile(operator.targetDirectory + fileName)
          }
          true
        case OverwriteStrategyEnum.overwriteWithWarning =>
          // Write the file and add a warning to the report
          if (fileExists) {
            executionReport.addOverwrittenFile(operator.targetDirectory + fileName)
            warningFiles.append(operator.targetDirectory + fileName)
          } else {
            executionReport.addNewFile(operator.targetDirectory + fileName)
          }
          true
        case OverwriteStrategyEnum.ignoreExisting if fileExists =>
          // Ignore the file if it already exists
          executionReport.addIgnoredFile(operator.targetDirectory + fileName)
          false
        case _ if fileExists =>
          // For any other strategy, throw an error if the file exists
          throw new IllegalArgumentException(s"Project file already exists: ${operator.targetDirectory}${fileName}")
        case _ =>
          // File doesn't exist, write it
          executionReport.addNewFile(operator.targetDirectory + fileName)
          true
      }
    }
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
  private val newFiles = mutable.Buffer[String]()

  private val overwrittenFiles = mutable.Buffer[String]()

  private val ignoredFiles = mutable.Buffer[String]()

  override def operationLabel: Option[String] = Some("add files")

  override def entityLabelSingle: String = "file"

  override def entityLabelPlural: String = "files"

  override def entityProcessVerb: String = "added"

  override def minEntitiesBetweenUpdates: Int = 1

  def addNewFile(file: String): Unit = {
    newFiles.append(file)
  }

  def addOverwrittenFile(file: String): Unit = {
    overwrittenFiles.append(file)
  }

  def addIgnoredFile(file: String): Unit = {
    ignoredFiles.append(file)
  }

  override def additionalFields(): Seq[(String, String)] = {
    Seq(
      "Number of new files" -> count(newFiles),
      "Number of overwritten files" -> count(overwrittenFiles),
      "Number of ignored files" -> count(ignoredFiles),
      "New files" -> list(newFiles),
      "Overwritten files" -> list(overwrittenFiles),
      "Ignored files" -> list(ignoredFiles),
    )
  }

  def list(files: mutable.Buffer[String]): String = {
    // files is still null when this method is called in the constructor of ExecutionReportUpdater
    if(files != null) files.mkString(", ") else ""
  }

  def count(files: mutable.Buffer[String]): String = {
    if(files != null) files.size.toString else "0"
  }
}