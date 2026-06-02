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
  description = "Writes a JSON string held in a field on each incoming entity to a file. Optionally packs all entities into a single ZIP archive. Produces a file entity downstream, suitable for wiring into a file-backed dataset or any operator that consumes file entities.",
  documentationFile = "JsonToFileTaskDocumentation.md",
  relatedPlugins = Array(
    new PluginReference(
      id = JsonParserTask.pluginId,
      description = "JSON to File writes the JSON string on each input entity to a file; Parse JSON parses the same kind of input into structured entities driven by a downstream schema."
    )
  )
)
case class JsonToFileOperator(@Param("The Silk path expression of the input entity that contains the JSON string. " +
                                  "If not set, the value of the first defined property will be taken.")
                              inputPath: String = "",
                              @Param("Filename for the produced file. If left empty, an auto-generated temporary name is used. " +
                                  "When the input contains more than one entity, an index suffix is appended before the extension " +
                                  "(e.g. `out-0.json`, `out-1.json`) to keep filenames unique. " +
                                  "In ZIP output mode, this name is used for the ZIP container file and as the base for entry names inside the archive.")
                              outputFileName: String = "",
                              @Param("MIME type of the produced file.")
                              mimeType: String = "application/json",
                              @Param("If enabled, all input entities are packed into a single ZIP file with one entry per entity. " +
                                  "When disabled (default), one file is produced per entity.")
                              zipOutput: Boolean = false,
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
