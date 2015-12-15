package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "xml",
  label = "XML",
  description =
      """Retrieves all entities from an xml file.
Parameters:
  file:  File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
  basePath: The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
            If left empty, all direct children of the root element will be read.
  uriPrefix: A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
"""
)
case class XmlDataset(file: Resource, basePath: String = "", uriPattern: String = "") extends DatasetPlugin {

  override def source: DataSource = new XmlSource(file, basePath, uriPattern)

  override def linkSink: LinkSink = throw new NotImplementedError("XMLs cannot be written at the moment")

  override def entitySink: EntitySink = throw new NotImplementedError("XMLs cannot be written at the moment")
}
