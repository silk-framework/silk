package org.silkframework.plugins.dataset.json

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.validation.ValidationException

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
  @Param(value = "The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.", advanced = true)
  basePath: String = "",
  @deprecated("This will be removed in the next release.", "")
  @Param(label = "URI pattern (deprecated)", value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
  uriPattern: String = "",
  @Param(label = "Output first entity as JSON object", value = "Specifies whether an Array or JSON objects is returned or a single object is returned for the first entity when writing to JSON files.", advanced = true)
  makeFirstEntityJsonObject: Boolean = false,
  template: MultilineStringParameter = "[{{entities}}]"
                      ) extends Dataset with ResourceBasedDataset {

  override def source(implicit userContext: UserContext): DataSource = JsonSource(file, basePath, uriPattern)

  override def linkSink(implicit userContext: UserContext): LinkSink = new TableLinkSink(new JsonSink(file))

  override def entitySink(implicit userContext: UserContext): EntitySink = new JsonSink(file, JsonTemplate.parse(template))
}

case class JsonTemplate(prefix: String, suffix: String)

object JsonTemplate {

  val default: JsonTemplate = JsonTemplate("[", "]")

  def parse(templateStr: String): JsonTemplate = {
    val parts = templateStr.split("""\{\{entities}}""")
    if(parts.length != 2) {
      throw new ValidationException("Template must contain {{entities}}.")
    }
    JsonTemplate(parts(0), parts(1))
  }

}