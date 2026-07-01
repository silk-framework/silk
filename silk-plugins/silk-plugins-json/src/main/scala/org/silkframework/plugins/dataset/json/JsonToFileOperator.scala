package org.silkframework.plugins.dataset.json

import org.silkframework.config.{CustomTask, FixedNumberOfInputs, FixedSchemaPort, FlexibleSchemaPort, InputPorts, Port}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.util.Uri

@Plugin(
  id = JsonToFileOperator.pluginId,
  label = "JSON to File",
  description = "Writes a JSON string held in a field on each valid incoming entity to a file. Depending on the output mode, it produces one file per entity, packs all entities into a single ZIP archive, or merges them into a single JSON array file. Produces a file entity downstream, suitable for wiring into a file-backed dataset or any operator that consumes file entities.",
  documentationFile = "JsonToFileTaskDocumentation.md",
  relatedPlugins = Array(
    new PluginReference(
      id = JsonParserTask.pluginId,
      description = "JSON to File writes the JSON string from each input entity to a file; Parse JSON parses the same kind of input into structured entities driven by a downstream schema."
    )
  )
)
case class JsonToFileOperator(@Param("The Silk path expression of the input entity that contains the JSON string. " +
                                  "If not set, the value of the first property in the entity schema will be taken.")
                              inputPath: String = "",
                              @Param("Filename for the produced file. If left empty, an auto-generated temporary name is used. " +
                                  "In file output mode, when the input contains more than one entity, an index suffix is appended " +
                                  "before the extension (e.g. `out-0.json`, `out-1.json`) to keep filenames unique. " +
                                  "In ZIP output mode, this name is used for the ZIP container file and as the base for entry names inside the archive. " +
                                  "In jsonArray output mode there is always a single output file, so the name is used as-is with no index suffix.")
                              outputFileName: String = "",
                              @Param("MIME type of the produced file.")
                              mimeType: String = "application/json",
                              @Param("Output mode: \"One file per entity\" writes one file per entity, \"ZIP archive\" packs all entities into a single ZIP archive, " +
                                  "\"Merged JSON array\" merges all entities into a single JSON array file.")
                              outputMode: JsonToFileOutputModeEnum = JsonToFileOutputModeEnum.file,
                              @Param("If set, the JSON value is wrapped in a JSON object under this property key before writing. " +
                                  "For example, with outputProperty set to 'payload', the input {\"name\":\"Alice\"} is written as " +
                                  "{\"payload\":{\"name\":\"Alice\"}}. When empty (default), the value is written as-is.")
                              outputProperty: String = "") extends CustomTask {

  val parsedInputPath: Option[UntypedPath] = {
    if (inputPath != "") {
      Some(UntypedPath.parse(inputPath))
    } else {
      None
    }
  }

  override lazy val inputPorts: InputPorts = {
    val inputPort = parsedInputPath match {
      case Some(path) =>
        FixedSchemaPort(
          EntitySchema(
            typeUri = Uri(""),
            typedPaths = IndexedSeq(path.asStringTypedPath)
          )
        )
      case None =>
        FlexibleSchemaPort()
    }
    FixedNumberOfInputs(Seq(inputPort))
  }

  override def outputPort: Option[Port] = {
    Some(FixedSchemaPort(FileEntitySchema.schema))
  }
}

object JsonToFileOperator {
  final val pluginId = "jsonToFile"
}
