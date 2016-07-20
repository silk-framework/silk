package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{WritableResource, Resource}

@Plugin(
  id = "xml",
  label = "XML",
  description =
"""Retrieves all entities from an xml file."""
)
case class XmlDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: Resource,
  @Param("The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.")
  basePath: String = "",
  @Param("A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements")
  uriPattern: String = "") extends DatasetPlugin {

  override def source: DataSource = new XmlSource(file, basePath, uriPattern)

  override def linkSink: LinkSink = throw new NotImplementedError("XMLs cannot be written at the moment")

  override def entitySink: EntitySink = throw new NotImplementedError("XMLs cannot be written at the moment")

  override def clear() = { }
}
