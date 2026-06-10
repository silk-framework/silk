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
import scala.collection.immutable.SeqMap
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
    val trimmedFileName = task.data.outputFileName.trim
    val entities = entityTable.entities.toIndexedSeq

    task.data.outputMode match {
      case JsonToFileOutputModeEnum.file      => executeFile(task, entities, pathIndex, trimmedFileName, context)
      case JsonToFileOutputModeEnum.zip       => executeZip(task, entities, pathIndex, trimmedFileName, context)
      case JsonToFileOutputModeEnum.jsonArray => executeMergedJson(task, entities, pathIndex, trimmedFileName, context)
      case _                        => throw TaskException(s"Unsupported output mode for 'JSON to File': ${task.data.outputMode}")
    }
  }

  /** Executes file mode: writes one file per valid entity and returns one FileEntity per valid entity. Entities whose
    * value is not valid JSON are skipped and recorded as warnings on the execution report. */
  private def executeFile(task: Task[JsonToFileOperator],
                          entities: IndexedSeq[Entity],
                          pathIndex: Int,
                          trimmedFileName: String,
                          context: ActivityContext[ExecutionReport])
                         (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(reserializedContent(_, pathIndex, outputProperty))
    val multiple = contents.size > 1
    val fileEntities = IndexedSeq.newBuilder[FileEntity]
    for ((finalString, index) <- contents.zipWithIndex) {
      val fileEntity = if (trimmedFileName.isEmpty) {
        FileEntity.createTemp(TempFilePrefix, ".json", mimeType = outputMimeType)
      } else {
        allocateNamedFileEntity(trimmedFileName, index, multiple, outputMimeType)
      }
      try {
        fileEntity.file.writeString(finalString)
      } catch {
        case NonFatal(e) =>
          Try(fileEntity.file.delete())
          throw e
      }
      fileEntities += fileEntity
    }
    reportSkipped(task, context, warnings, contents.size)
    Some(FileEntitySchema.create(fileEntities.result(), task))
  }

  /** Executes merged JSON mode: merges all valid entities into a single JSON array file and returns one FileEntity.
    * Each element's original JSON text is written through unchanged. Parsing only validates the input; the parsed
    * node is discarded, so numbers, key order, duplicate keys, and per-element formatting are preserved exactly,
    * matching the byte preservation of file and zip modes. Entities whose value is not valid JSON are skipped and
    * recorded as warnings on the execution report. */
  private def executeMergedJson(task: Task[JsonToFileOperator],
                               entities: IndexedSeq[Entity],
                               pathIndex: Int,
                               trimmedFileName: String,
                               context: ActivityContext[ExecutionReport])
                              (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (entities.isEmpty) {
      return Some(FileEntitySchema.create(Seq.empty, task))
    }
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(rawContent(_, pathIndex, outputProperty))
    val serialized = contents.mkString("[", ",", "]")
    val fileEntity = if (trimmedFileName.isEmpty) {
      FileEntity.createTemp(TempFilePrefix, ".json", mimeType = outputMimeType)
    } else {
      allocateNamedFileEntity(trimmedFileName, 0, multiple = false, outputMimeType)
    }
    try {
      fileEntity.file.writeString(serialized)
    } catch {
      case NonFatal(e) =>
        Try(fileEntity.file.delete())
        throw e
    }
    reportSkipped(task, context, warnings, contents.size)
    Some(FileEntitySchema.create(Seq(fileEntity), task))
  }

  /** Executes ZIP mode: writes all valid entities as entries into a single ZIP file and returns one FileEntity.
    * Entities whose value is not valid JSON are skipped and recorded as warnings on the execution report. */
  private def executeZip(task: Task[JsonToFileOperator],
                         entities: IndexedSeq[Entity],
                         pathIndex: Int,
                         trimmedFileName: String,
                         context: ActivityContext[ExecutionReport])
                        (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (entities.isEmpty) {
      return Some(FileEntitySchema.create(Seq.empty, task))
    }
    val effectiveMimeType = if (task.data.mimeType == "application/json") "application/zip" else task.data.mimeType
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(reserializedContent(_, pathIndex, outputProperty))
    val multiple = contents.size > 1
    val zipFileEntity = if (trimmedFileName.isEmpty) {
      FileEntity.createTemp(TempFilePrefix, ".zip", mimeType = Some(effectiveMimeType))
    } else {
      allocateNamedFileEntity(trimmedFileName, 0, multiple = false, Some(effectiveMimeType))
    }
    try {
      zipFileEntity.file.write() { outputStream =>
        val zip = new ZipOutputStream(outputStream)
        for ((finalString, index) <- contents.zipWithIndex) {
          val entryName = if (trimmedFileName.nonEmpty) {
            if (multiple) suffixedName(trimmedFileName, index) else trimmedFileName
          } else {
            if (multiple) suffixedName("entry.json", index) else "entry.json"
          }
          val entryResource = ZipOutputStreamResource(entryName, entryName, zip)
          entryResource.writeString(finalString)
        }
        zip.finish()
      }
      reportSkipped(task, context, warnings, contents.size)
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

  /** Parses the input string into a JsonNode; throws TaskException if the string is not valid JSON. */
  private def parseJson(jsonString: String): JsonNode = {
    try {
      JsonNodeSerializer.parse(jsonString)
    } catch {
      case ex: JsonParseException =>
        throw TaskException(s"Input value for 'JSON to File' operator is not valid JSON: ${ex.getMessage}")
    }
  }

  /** Reads the input value and validates that it parses as JSON, returning the original text. The parsed node is
    * discarded so the raw bytes survive into the merged output. */
  private def readValidatedJson(entity: Entity, pathIndex: Int): String = {
    val raw = readJsonValue(entity, pathIndex)
    parseJson(raw)
    raw
  }

  /** Validates and returns the final string to write: the original when outputProperty is empty, or the JSON value
    * wrapped in an object under the given key when outputProperty is set. Always validates the input as JSON. */
  private def applyOutputProperty(jsonString: String, outputProperty: String): String = {
    val node = parseJson(jsonString)
    if (outputProperty.isEmpty) {
      jsonString
    } else {
      // JsonPosition(1, 1): synthetic — this node has no source location.
      JsonNodeSerializer.toString(JsonObject(SeqMap(outputProperty -> node), JsonPosition(1, 1)))
    }
  }

  /** Wraps a raw JSON value under the output property as a textual object, splicing the value verbatim, or returns it
    * unchanged when no property is set. Unlike applyOutputProperty the raw bytes are preserved (no re-serialization). */
  private def wrapRawValue(raw: String, outputProperty: String): String = {
    if (outputProperty.isEmpty) {
      raw
    } else {
      val key = JsonNodeSerializer.toString(JsonString(outputProperty, JsonPosition(1, 1)))
      s"{$key:$raw}"
    }
  }

  /** Per-entity content for file and zip modes: validate and wrap via re-serialization. */
  private def   reserializedContent(entity: Entity, pathIndex: Int, outputProperty: String): String =
    applyOutputProperty(readJsonValue(entity, pathIndex), outputProperty)

  /** Per-entity content for merged mode: validate, preserving the original bytes. */
  private def rawContent(entity: Entity, pathIndex: Int, outputProperty: String): String =
    wrapRawValue(readValidatedJson(entity, pathIndex), outputProperty)

  /** Turns each entity into its output string via `content`, skipping entities whose value fails validation
    * (`TaskException`) and collecting one warning per skip. Non-validation errors propagate. */
  private def partitionValid(entities: IndexedSeq[Entity])
                            (content: Entity => String): (IndexedSeq[String], Seq[String]) = {
    val (warnings, valid) = entities.partitionMap { entity =>
      try Right(content(entity))
      catch { case ex: TaskException => Left(s"Skipped entity ${entity.uri}: ${ex.getMessage}") }
    }
    (valid, warnings)
  }

  /** Records the skipped entities as warnings on the execution report. */
  private def reportSkipped(task: Task[JsonToFileOperator],
                           context: ActivityContext[ExecutionReport],
                           warnings: Seq[String],
                           validCount: Int): Unit = {
    if (warnings.nonEmpty) {
      context.value.update(SimpleExecutionReport(task, warnings = warnings, entityCount = validCount, isDone = true))
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

object LocalJsonToFileOperatorExecutor {
  /** Prefix for temp files produced by this operator. */
  private val TempFilePrefix = "jsonOutput"
}
