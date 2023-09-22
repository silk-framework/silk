package org.silkframework.plugins.dataset.json

import org.silkframework.config.CustomTask
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.Uri

@Plugin(
  id = "JsonParserOperator",
  label = "Parse JSON",
  description = "Parses incoming entities as a JSON dataset. Typically, it is used before a transformation task. Takes exactly one input."
)
case class JsonParserTask(@Param("The Silk path expression of the input entity that contains the JSON document. If " +
    "not set, the value of the first defined property will be taken.")
                          inputPath: String = "",
                          @Param("The path to the elements to be read, starting from the root element, " +
                              "e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.")
                          basePath: String = "",
                          @Param(
                            label = "URI suffix pattern",
                            value = "A URI pattern that is relative to the base URI of the input entity, e.g., /{ID}, " +
                              "where {path} may contain relative paths to elements. This relative part is appended to the input entity URI to construct the full URI pattern.")
                          uriSuffixPattern: String = "") extends CustomTask {
  val parsedInputPath: Option[UntypedPath] = {
    if (inputPath != "") {
      Some(UntypedPath.parse(inputPath))
    } else {
      None
    }
  }

  /**
    * If no inputPath is defined then we don't care about the input schema and take the value of the first input path.
    * Else we pick the input path of the entity that matches inputPath.
    */
  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = {
    parsedInputPath map { path =>
      Seq(
        EntitySchema(
          typeUri = Uri(""),
          typedPaths = IndexedSeq(path.asStringTypedPath)
        )
      )
    }
  }

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override lazy val outputSchemaOpt: Option[EntitySchema] = None
}
