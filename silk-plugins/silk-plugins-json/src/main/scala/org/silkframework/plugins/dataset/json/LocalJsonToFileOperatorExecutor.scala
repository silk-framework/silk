package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.JsonParseException
import org.silkframework.config.Task
import org.silkframework.entity.Entity
import org.silkframework.execution._
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.zip.ZipOutputStreamResource

import java.util.zip.ZipOutputStream
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Executor for [[JsonToFileOperator]]. Takes a single input table and iterates over every entity in it, reading the
  * JSON string at the configured input path on each and writing it to a file. When ZIP output is enabled, all entities
  * are packed into a single ZIP archive with one entry per entity instead. The produced file is wrapped in a
  * [[FileEntity]] and surfaced downstream via [[FileEntitySchema]].
  */
case class LocalJsonToFileOperatorExecutor() extends LocalExecutor[JsonToFileOperator] {

  override def execute(task: Task[JsonToFileOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (inputs.size != 1) throw TaskException("'JSON to File' takes exactly one input!")
    val entityTable = inputs.head
    val pathIndex = task.data.parsedInputPath match {
      case Some(path) => entityTable.entitySchema.indexOfPath(path)
      case None => 0 // Take the value of the first path
    }
    val trimmedFileName = task.data.outputFileName.trim
    val entities = entityTable.entities.toIndexedSeq

    if (task.data.zipOutput) {
      executeZip(task, entities, pathIndex, trimmedFileName)
    } else {
      val outputMimeType = Some(task.data.mimeType)
      val multiple = entities.size > 1
      val fileEntities = entities.zipWithIndex.map { case (entity, index) =>
        val jsonString = readJsonValue(entity, pathIndex)
        validateJson(jsonString)
        val fileEntity = if (trimmedFileName.isEmpty) {
          FileEntity.createTemp("jsonOutput", ".json", mimeType = outputMimeType)
        } else {
          allocateNamedFileEntity(trimmedFileName, index, multiple, outputMimeType)
        }
        try {
          fileEntity.file.writeString(jsonString)
        } catch {
          case NonFatal(e) =>
            Try(fileEntity.file.delete())
            throw e
        }
        fileEntity
      }
      Some(FileEntitySchema.create(fileEntities, task))
    }
  }

  /** Executes ZIP mode: writes all entities as entries into a single ZIP file and returns one FileEntity. */
  private def executeZip(task: Task[JsonToFileOperator],
                         entities: IndexedSeq[Entity],
                         pathIndex: Int,
                         trimmedFileName: String)
                        (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (entities.isEmpty) {
      return Some(FileEntitySchema.create(Seq.empty, task))
    }
    val effectiveMimeType = if (task.data.mimeType == "application/json") "application/zip" else task.data.mimeType
    val multiple = entities.size > 1
    val zipFileEntity = if (trimmedFileName.isEmpty) {
      FileEntity.createTemp("jsonOutput", ".zip", mimeType = Some(effectiveMimeType))
    } else {
      allocateNamedFileEntity(trimmedFileName, 0, multiple = false, Some(effectiveMimeType))
    }
    try {
      zipFileEntity.file.write() { outputStream =>
        val zip = new ZipOutputStream(outputStream)
        for ((entity, index) <- entities.zipWithIndex) {
          val jsonString = readJsonValue(entity, pathIndex)
          validateJson(jsonString)
          val entryName = if (trimmedFileName.nonEmpty) {
            if (multiple) suffixedName(trimmedFileName, index) else trimmedFileName
          } else {
            if (multiple) suffixedName("entry.json", index) else "entry.json"
          }
          val entryResource = ZipOutputStreamResource(entryName, entryName, zip)
          entryResource.writeString(jsonString)
        }
        zip.finish()
      }
      Some(FileEntitySchema.create(Seq(zipFileEntity), task))
    } catch {
      case NonFatal(e) =>
        Try(zipFileEntity.file.delete())
        throw e
    }
  }

  /** Reads the JSON string from the configured field on an input entity. */
  private def readJsonValue(entity: Entity, pathIndex: Int): String = {
    val values = entity.values
    if (values.size <= pathIndex) {
      throw TaskException(s"No input value at path index $pathIndex found for 'JSON to File' operator on entity ${entity.uri}.")
    }
    values(pathIndex).headOption.filter(_.nonEmpty).getOrElse {
      throw TaskException(s"No JSON value at the configured input path for 'JSON to File' operator on entity ${entity.uri}.")
    }
  }

  /** Parses the input string with Jackson to confirm it is valid JSON; throws TaskException otherwise. */
  private def validateJson(jsonString: String): Unit = {
    try {
      JsonNodeSerializer.parse(jsonString)
    } catch {
      case ex: JsonParseException =>
        throw TaskException(s"Input value for 'JSON to File' operator is not valid JSON: ${ex.getMessage}")
    }
  }

  /** Allocates a file entity with a caller-supplied name in the system temp directory. */
  private def allocateNamedFileEntity(name: String, index: Int, multiple: Boolean, mimeType: Option[String]): FileEntity = {
    val finalName = if (multiple) suffixedName(name, index) else name
    FileEntity.createTemp("", name = Some(finalName), mimeType = mimeType)
  }

  /** Inserts an index suffix before the file extension; appends it if there is no extension. */
  private def suffixedName(name: String, index: Int): String = {
    val dotIdx = name.lastIndexOf('.')
    if (dotIdx > 0) {
      name.substring(0, dotIdx) + "-" + index + name.substring(dotIdx)
    } else {
      name + "-" + index
    }
  }
}
