package org.silkframework.plugins.dataset.json

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.Resource

import scala.io.Codec

@Plugin(
  id = "json",
  label = "JSON",
  description =
"""Retrieves all entities from an JSON file."""
)
case class JsonDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: Resource,
  @Param("The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.")
  basePath: String = "",
  @Param("A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements")
  uriPattern: String = "",
  @Param("The file encoding, e.g., UTF8, ISO-8859-1")
  charset: String = "UTF8") extends DatasetPlugin {

  private val codec = Codec(charset)

  override def source: DataSource = new JsonSource(file, basePath, uriPattern, codec)

  override def linkSink: LinkSink = throw new NotImplementedError("JSON files cannot be written at the moment")

  override def entitySink: EntitySink = throw new NotImplementedError("JSON files cannot be written at the moment")

  override def clear() = { }
}
