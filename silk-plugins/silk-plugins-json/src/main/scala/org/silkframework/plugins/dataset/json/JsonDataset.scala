package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.DatasetCharacteristics.{SpecialPathInfo, SuggestedForEnum, SupportedPathExpressions}
import org.silkframework.dataset._
import org.silkframework.plugins.dataset.hierarchical.HierarchicalSink.DEFAULT_MAX_SIZE
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.JsonCodeParameter
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Identifier

@Plugin(
  id = "json",
  label = "JSON",
  categories = Array(DatasetCategories.file),
  description = """Read from or write to a JSON file.""",
  documentationFile = "JsonDatasetDocumentation.md"
)
case class JsonDataset(
                        @Param("Json file.")
                        file: WritableResource,
                        @Param("Template for writing JSON. The term {{output}} will be replaced by the written JSON.")
                        template: JsonCodeParameter = JsonCodeParameter(s"${JsonTemplate.placeholder}"),
                        @Param(value = "The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.", advanced = true)
                        basePath: String = "",
                        @deprecated("This will be removed in the next release.", "")
                        @Param(label = "URI pattern (deprecated)", value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
                        uriPattern: String = "",
                        @Param(value = "Maximum depth of written JSON. This acts as a safe guard if a recursive structure is written.", advanced = true)
                        maxDepth: Int = DEFAULT_MAX_SIZE,
                        @Param(value = "Streaming allows for reading large JSON files. If streaming is enabled, backward paths are not supported.", advanced = true)
                        streaming: Boolean = true) extends Dataset with ResourceBasedDataset {

  private val jsonTemplate = JsonTemplate.parse(template)

  override def source(implicit userContext: UserContext): DataSource = {
    if(streaming) {
      new JsonSourceStreaming(Identifier.fromAllowed(file.name), file, basePath, uriPattern)
    } else {
      file.checkSizeForInMemory()
      JsonSourceInMemory(file, basePath, uriPattern)
    }
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = new TableLinkSink(new JsonSink(file, maxDepth = maxDepth))

  override def entitySink(implicit userContext: UserContext): EntitySink = new JsonSink(file, jsonTemplate, maxDepth)

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics(
    SupportedPathExpressions(
      multiHopPaths = true,
      backwardPaths = true,
      propertyFilter = true,
      specialPaths = Seq(
        SpecialPathInfo(JsonDataset.specialPaths.ID, Some("Hash value of the JSON node or value."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(JsonDataset.specialPaths.TEXT,
          Some("The string value of a node. This will turn a JSON object into it's string representation."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(JsonDataset.specialPaths.BACKWARD_PATH, Some("Navigates back to parent object.")),
        SpecialPathInfo(JsonDataset.specialPaths.LINE, Some("Line number of the selected JSON node."), SuggestedForEnum.ValuePathOnly),
        SpecialPathInfo(JsonDataset.specialPaths.COLUMN, Some("Column position of the selected JSON node."), SuggestedForEnum.ValuePathOnly)
      )
    )
  )
}

object JsonDataset {

  object specialPaths {
    final val TEXT = "#text"
    final val ID = "#id"
    final val BACKWARD_PATH = "\\.."
    final val LINE = "#line"
    final val COLUMN = "#column"
    final val all = Seq(ID, TEXT, BACKWARD_PATH, LINE, COLUMN)
  }

}
