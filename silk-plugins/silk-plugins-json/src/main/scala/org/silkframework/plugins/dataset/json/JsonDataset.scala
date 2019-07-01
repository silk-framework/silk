package org.silkframework.plugins.dataset.json

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.Resource

import scala.io.Codec

@Plugin(
  id = "json",
  label = "JSON file",
  categories = Array(DatasetCategories.file),
  description =
"""Retrieves all entities from an JSON file."""
)
case class JsonDataset(
  @Param("Json file.")
  file: Resource,
  @Param("The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.")
  basePath: String = "",
  @deprecated("This will be removed in the next release.", "")
  @Param("A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements")
  uriPattern: String = "",
  @Param("The file encoding, e.g., UTF8, ISO-8859-1")
  charset: String = "UTF8") extends Dataset with ResourceBasedDataset {

  private val codec = Codec(charset)

  override def source(implicit userContext: UserContext): DataSource = JsonSource(file, basePath, uriPattern, codec)

  override def linkSink(implicit userContext: UserContext): LinkSink = throw new NotImplementedError("JSON files cannot be written at the moment")

  override def entitySink(implicit userContext: UserContext): EntitySink = throw new NotImplementedError("JSON files cannot be written at the moment")
}
