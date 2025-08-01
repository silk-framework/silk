package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.DatasetCharacteristics.{SpecialPathInfo, SpecialPaths, SuggestedForEnum, SupportedPathExpressions}
import org.silkframework.dataset._
import org.silkframework.dataset.bulk.TextBulkResourceBasedDataset
import org.silkframework.plugins.dataset.hierarchical.HierarchicalSink.DEFAULT_MAX_SIZE
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.JsonCodeParameter
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.util.Identifier

import java.nio.charset.StandardCharsets
import scala.io.Codec

@Plugin(
  id = "json",
  label = "JSON",
  categories = Array(DatasetCategories.file),
  description = """Read from or write to a JSON or JSON Lines file.""",
  documentationFile = "JsonDatasetDocumentation.md"
)
case class JsonDataset(@Param("JSON file. This may also be a zip archive of multiple JSON files that share the same schema.")
                       file: WritableResource,
                       @Param("Template for writing JSON. The term {{output}} will be replaced by the written JSON.")
                       template: JsonCodeParameter = JsonCodeParameter(s"${JsonTemplate.placeholder}"),
                       @Param("Navigate into arrays automatically. If set to false, the `#array` path operator must be used to navigate into arrays.")
                       navigateIntoArrays: Boolean = true,
                       @Param(value = "The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.", advanced = true)
                       basePath: String = "",
                       @deprecated("This will be removed in the next release.", "")
                       @Param(label = "URI pattern (deprecated)", value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
                       uriPattern: String = "",
                       @Param(value = "Maximum depth of written JSON. This acts as a safe guard if a recursive structure is written.", advanced = true)
                       maxDepth: Int = DEFAULT_MAX_SIZE,
                       @Param(value = "Streaming allows for reading large JSON files. If streaming is enabled, backward paths are not supported.", advanced = true)
                       streaming: Boolean = true,
                       @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
                       override val zipFileRegex: String = JsonDataset.defaultZipFileRegex) extends Dataset with TextBulkResourceBasedDataset {

  private val jsonTemplate = JsonTemplate.parse(template)

  override def codec: Codec = StandardCharsets.UTF_8

  override def mimeType: Option[String] = Some("application/json")

  override def mergeSchemata: Boolean = true

  override def createSource(resource: Resource): DataSource = {
    if (streaming) {
      new JsonSourceStreaming(Identifier.fromAllowed(resource.name), resource, basePath, uriPattern, navigateIntoArrays)
    }
    else {
      // The maxInMemorySize limit will be checked by the JsonReader class
      JsonSourceInMemory.fromResource(resource, basePath, uriPattern, navigateIntoArrays)
    }
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = new TableLinkSink(new JsonSink(bulkWritableResource, maxDepth = maxDepth))

  override def entitySink(implicit userContext: UserContext): EntitySink = new JsonSink(bulkWritableResource, jsonTemplate, maxDepth)

  override def characteristics: DatasetCharacteristics = JsonDataset.characteristics
}

object JsonDataset {

  final val defaultZipFileRegex = """^(?!.*[\/\\]\..*$|^\..*$).*\.jsonl?$"""

  object specialPaths {
    final val TEXT = "#text"
    final val ARRAY_TEXT = "#arrayText"
    final val ID = "#id"
    final val UUID = "#uuid"
    final val KEY = "#key"
    final val ARRAY = "#array"
    final val ALL_CHILDREN = "*"
    final val BACKWARD_PATH = "\\.."
  }

  final val characteristics: DatasetCharacteristics = DatasetCharacteristics(
    SupportedPathExpressions(
      multiHopPaths = true,
      backwardPaths = true,
      propertyFilter = true,
      specialPaths = Seq(
        SpecialPathInfo(specialPaths.ID, Some("Hash value of the JSON node or value."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(specialPaths.UUID, Some("Generates a type 3 (name based) UUID from the string representation of the JSON node or value."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(specialPaths.TEXT,
          Some("The string value of a node. This will turn a JSON object into it's string representation."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(specialPaths.ARRAY_TEXT,
          Some("The string value of a node. If the parent of the node is an array it will return the string represenation of the array instead."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(specialPaths.KEY, Some("The name of the current object key"), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(specialPaths.ARRAY, Some("Navigate into an array. If 'navigate into arrays' is set, this path is not needed.")),
        SpecialPaths.LINE,
        SpecialPaths.COLUMN,
        SpecialPathInfo(specialPaths.ALL_CHILDREN, Some("Selects all direct children of the entity.")),
        SpecialPathInfo(specialPaths.BACKWARD_PATH, Some("Navigates back to parent object."))
      )
    )
  )

}
