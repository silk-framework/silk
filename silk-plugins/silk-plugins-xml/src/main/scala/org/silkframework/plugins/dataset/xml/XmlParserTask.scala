package org.silkframework.plugins.dataset.xml

import org.silkframework.config.CustomTask
import org.silkframework.entity.{Path, EntitySchema}
import org.silkframework.runtime.plugin.{Plugin, Param}
import org.silkframework.util.Uri

/**
  * Created on 8/22/16.
  */
@Plugin(
  id = "XmlParserOperator",
  label = "XML Parser Operator",
  description = "Takes exactly one input and reads either the defined inputPath or the first value of the first entity as " +
      "XML document. Then executes the given output entity schema similar to the XML dataset to construct the result entities."
)
case class XmlParserTask(@Param("The Silk path expression of the input entity that contains the XML document. If not set, " +
    "the value of the first defined property will be taken.")
                         inputPath: String = "",
                         @Param("The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. " +
                             "If left empty, all direct children of the root element will be read.")
                         basePath: String = "",
                         @Param("A URI pattern that is relative to the base URI of the input entity, e.g., /{ID}, where {path} " +
                             "may contain relative paths to elements. This relative part is appended to the input entity URI to " +
                             "construct the full URI pattern.")
                         uriSuffixPattern: String = "") extends CustomTask {
  val parsedInputPath = {
    if (inputPath != "") {
      Some(Path.parse(inputPath))
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
          paths = IndexedSeq(path)
        )
      )
    }
  }

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override lazy val outputSchemaOpt: Option[EntitySchema] = None // This works like a dataset and can handle arbitrary entity schemata
}
