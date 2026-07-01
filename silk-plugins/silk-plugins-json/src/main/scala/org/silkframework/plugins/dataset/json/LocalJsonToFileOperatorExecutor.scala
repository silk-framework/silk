package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.JsonParseException
import org.silkframework.config.Task
import org.silkframework.entity.Entity
import org.silkframework.execution._
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.zip.ZipOutputStreamResource

import java.nio.charset.StandardCharsets
import java.util.zip.ZipOutputStream
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Executor for [[JsonToFileOperator]]. Takes a single input table and iterates over every entity in it, reading the
  * JSON string at the configured input path on each. The output mode selects what is produced: `file` writes one file
  * per entity, `zip` packs all entities into a single ZIP archive, and `jsonArray` merges all entities into a single
  * JSON array file. The produced file(s) are wrapped in [[FileEntity]] and surfaced downstream via [[FileEntitySchema]].
  */
case class LocalJsonToFileOperatorExecutor() extends LocalExecutor[JsonToFileOperator] {

  import LocalJsonToFileOperatorExecutor.TempFilePrefix

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
    task.data.outputMode match {
      case JsonToFileOutputModeEnum.file      => executeFile(task, entityTable.entities, pathIndex, context)
      case JsonToFileOutputModeEnum.zip       => executeZip(task, entityTable.entities, pathIndex, context)
      case JsonToFileOutputModeEnum.jsonArray => executeMergedJson(task, entityTable.entities, pathIndex, context)
      case _                        => throw TaskException(s"Unsupported output mode for 'JSON to File': ${task.data.outputMode}")
    }
  }

  private def executeFile(task: Task[JsonToFileOperator],
                          entities: CloseableIterator[Entity],
                          pathIndex: Int,
                          context: ActivityContext[ExecutionReport])
                         (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val fileEntities = IndexedSeq.newBuilder[FileEntity]
    val (validCount, warnings) = processSkippingInvalid(entities, pathIndex, outputProperty) { content =>
      fileEntities += writeTempFile(".json", outputMimeType, content)
    }
    report(task, context, warnings, validCount)
    Some(FileEntitySchema.create(fileEntities.result(), task))
  }

  /** The `[`, `,`, and `]` bytes are written by hand as content arrives; nothing re-parses the assembled file
    * afterward, so the array's well-formedness depends entirely on this loop's bracket and comma placement. */
  private def executeMergedJson(task: Task[JsonToFileOperator],
                               entities: CloseableIterator[Entity],
                               pathIndex: Int,
                               context: ActivityContext[ExecutionReport])
                              (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val fileEntity = FileEntity.createTemp(TempFilePrefix, ".json").copy(mimeType = outputMimeType)
    try {
      val (validCount, warnings) = fileEntity.file.write() { outputStream =>
        outputStream.write("[".getBytes(StandardCharsets.UTF_8))
        var isFirst = true
        val result = processSkippingInvalid(entities, pathIndex, outputProperty) { content =>
          if (!isFirst) outputStream.write(",".getBytes(StandardCharsets.UTF_8))
          outputStream.write(content.getBytes(StandardCharsets.UTF_8))
          isFirst = false
        }
        outputStream.write("]".getBytes(StandardCharsets.UTF_8))
        result
      }
      report(task, context, warnings, validCount)
      Some(FileEntitySchema.create(Seq(fileEntity), task))
    } catch {
      case NonFatal(e) =>
        Try(fileEntity.file.delete())
        throw e
    }
  }

  /** Entries are always `entry-N.json`; a single valid entity is never named `entry.json`, since streaming
    * never knows the total valid count before writing the first entry. */
  private def executeZip(task: Task[JsonToFileOperator],
                         entities: CloseableIterator[Entity],
                         pathIndex: Int,
                         context: ActivityContext[ExecutionReport])
                        (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val effectiveMimeType = if (task.data.mimeType == "application/json") "application/zip" else task.data.mimeType
    val outputProperty = task.data.outputProperty
    val zipFileEntity = FileEntity.createTemp(TempFilePrefix, ".zip").copy(mimeType = Some(effectiveMimeType))
    try {
      val (validCount, warnings) = zipFileEntity.file.write() { outputStream =>
        val zip = new ZipOutputStream(outputStream)
        var index = 0
        val result = processSkippingInvalid(entities, pathIndex, outputProperty) { content =>
          val entryName = s"entry-$index.json"
          val entryResource = ZipOutputStreamResource(entryName, entryName, zip)
          entryResource.writeString(content)
          index += 1
        }
        zip.finish()
        result
      }
      report(task, context, warnings, validCount)
      Some(FileEntitySchema.create(Seq(zipFileEntity), task))
    } catch {
      case NonFatal(e) =>
        Try(zipFileEntity.file.delete())
        throw e
    }
  }

  private def readJsonValue(entity: Entity, pathIndex: Int): String = {
    val values = entity.values
    if (values.size <= pathIndex) {
      throw TaskException(s"No input value at path index $pathIndex found for 'JSON to File' operator on entity ${entity.uri}.")
    }
    values(pathIndex).headOption.filter(_.nonEmpty).getOrElse {
      throw TaskException(s"No JSON value at the configured input path for 'JSON to File' operator on entity ${entity.uri}.")
    }
  }

  /** Parses the input string into a JsonNode; throws TaskException if the string is not valid JSON. */
  private def parseJson(jsonString: String): JsonNode = {
    try {
      JsonNodeSerializer.parse(jsonString)
    } catch {
      case ex: JsonParseException =>
        throw TaskException(s"Input value for 'JSON to File' operator is not valid JSON: ${ex.getMessage}")
    }
  }

  /** Per-entity content for all modes: read the value, validate it parses as a single JSON document (the parsed node
    * is discarded so the raw bytes survive), and wrap it under outputProperty when set, splicing the value verbatim. */
  private def rawContent(entity: Entity, pathIndex: Int, outputProperty: String): String = {
    val raw = readJsonValue(entity, pathIndex)
    parseJson(raw) // validate only; the node is discarded so the original bytes are preserved
    if (outputProperty.isEmpty) {
      raw
    } else {
      val key = JsonNodeSerializer.toString(JsonString(outputProperty, JsonPosition(1, 1)))
      s"{$key:$raw}"
    }
  }

  /** Hands each valid entity's content to `onValid` and moves on without holding onto it — no content from more
    * than one entity is ever live at once, regardless of how many entities the source yields. */
  private def processSkippingInvalid(entities: CloseableIterator[Entity], pathIndex: Int, outputProperty: String)
                                     (onValid: String => Unit): (Int, Seq[String]) = {
    val warnings = ArrayBuffer[String]()
    var validCount = 0
    for (entity <- entities) {
      try {
        onValid(rawContent(entity, pathIndex, outputProperty))
        validCount += 1
      } catch {
        case ex: TaskException => warnings += s"Skipped entity ${entity.uri}: ${ex.getMessage}"
      }
    }
    (validCount, warnings.toSeq)
  }

  private def report(task: Task[JsonToFileOperator],
                     context: ActivityContext[ExecutionReport],
                     warnings: Seq[String],
                     validCount: Int): Unit = {
    context.value.update(SimpleExecutionReport(task, warnings = warnings, entityCount = validCount, isDone = true))
  }

  /** Writes content to a new temp file and returns its FileEntity; deletes the temp file and rethrows on write failure. */
  private def writeTempFile(suffix: String, mimeType: Option[String], content: String): FileEntity = {
    val fileEntity = FileEntity.createTemp(TempFilePrefix, suffix).copy(mimeType = mimeType)
    try {
      fileEntity.file.writeString(content)
      fileEntity
    } catch {
      case NonFatal(e) =>
        Try(fileEntity.file.delete())
        throw e
    }
  }

}

object LocalJsonToFileOperatorExecutor {
  /** Prefix for temp files produced by this operator. */
  private val TempFilePrefix = "jsonOutput"
}
