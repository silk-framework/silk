package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.{DataSink, DataSource, DatasetPlugin}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.Resource

import scala.io.Codec

@Plugin(
  id = "json",
  label = "JSON",
  description =
      """Retrieves all entities from an JSON file.
Parameters:
  file:  File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
  basePath: The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
            If left empty, all direct children of the root element will be read.
  uriPrefix: A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
"""
)
case class JsonDataset(file: Resource, basePath: String = "", uriPattern: String = "", charset: String = "UTF8") extends DatasetPlugin {

  private val codec = Codec(charset)

  override def source: DataSource = new JsonSource(file, basePath, uriPattern, codec)

  override def sink: DataSink = throw new NotImplementedError("JSON files cannot be written at the moment")
}
