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
    val entities = entityTable.entities.toIndexedSeq

    task.data.outputMode match {
      case JsonToFileOutputModeEnum.file      => executeFile(task, entities, pathIndex, context)
      case JsonToFileOutputModeEnum.zip       => executeZip(task, entities, pathIndex, context)
      case JsonToFileOutputModeEnum.jsonArray => executeMergedJson(task, entities, pathIndex, context)
      case _                        => throw TaskException(s"Unsupported output mode for 'JSON to File': ${task.data.outputMode}")
    }
  }

  /** Executes file mode: writes one file per valid entity and returns one FileEntity per valid entity. Entities whose
    * value is not valid JSON are skipped and recorded as warnings on the execution report. */
  private def executeFile(task: Task[JsonToFileOperator],
                          entities: IndexedSeq[Entity],
                          pathIndex: Int,
                          context: ActivityContext[ExecutionReport])
                         (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(rawContent(_, pathIndex, outputProperty))
    val fileEntities = IndexedSeq.newBuilder[FileEntity]
    for (content <- contents) {
      fileEntities += writeTempFile(".json", outputMimeType, content)
    }
    report(task, context, warnings, contents.size)
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
                               context: ActivityContext[ExecutionReport])
                              (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val outputMimeType = Some(task.data.mimeType)
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(rawContent(_, pathIndex, outputProperty))
    val serialized = contents.mkString("[", ",", "]")
    val fileEntity = writeTempFile(".json", outputMimeType, serialized)
    report(task, context, warnings, contents.size)
    Some(FileEntitySchema.create(Seq(fileEntity), task))
  }

  /** Executes ZIP mode: writes all valid entities as entries into a single ZIP file and returns one FileEntity.
    * Entities whose value is not valid JSON are skipped and recorded as warnings on the execution report. */
  private def executeZip(task: Task[JsonToFileOperator],
                         entities: IndexedSeq[Entity],
                         pathIndex: Int,
                         context: ActivityContext[ExecutionReport])
                        (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val effectiveMimeType = if (task.data.mimeType == "application/json") "application/zip" else task.data.mimeType
    val outputProperty = task.data.outputProperty
    val (contents, warnings) = partitionValid(entities)(rawContent(_, pathIndex, outputProperty))
    val multiple = contents.size > 1
    val zipFileEntity = FileEntity.createTemp(TempFilePrefix, ".zip").copy(mimeType = Some(effectiveMimeType))
    try {
      zipFileEntity.file.write() { outputStream =>
        val zip = new ZipOutputStream(outputStream)
        for ((content, index) <- contents.zipWithIndex) {
          val entryName = if (multiple) s"entry-$index.json" else "entry.json"
          val entryResource = ZipOutputStreamResource(entryName, entryName, zip)
          entryResource.writeString(content)
        }
        zip.finish()
      }
      report(task, context, warnings, contents.size)
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

  /** Sets the execution report: the valid entity count, completion, and one warning per skipped entity. */
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
