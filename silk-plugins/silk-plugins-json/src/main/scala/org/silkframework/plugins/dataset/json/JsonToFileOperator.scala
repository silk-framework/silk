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
  description = "Writes a JSON string carried as a field value on each incoming entity to a file. Produces a file entity downstream, suitable for wiring into a file-backed dataset or any operator that consumes file entities.",
  relatedPlugins = Array(
    new PluginReference(
      id = JsonParserTask.pluginId,
      description = "JSON to File persists the JSON string on each input entity as a file artifact; Parse JSON parses the same kind of input into structured entities driven by a downstream schema."
    )
  )
)
case class JsonToFileOperator(@Param("The Silk path expression of the input entity that contains the JSON string. " +
                                  "If not set, the value of the first defined property will be taken.")
                              inputPath: String = "",
                              @Param("Filename for the produced file. If left empty, an auto-generated temporary name is used. " +
                                  "When the input carries more than one entity, an index suffix is appended before the extension " +
                                  "(e.g. `out-0.json`, `out-1.json`) to keep filenames unique.")
                              outputFileName: String = "") extends CustomTask {

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
