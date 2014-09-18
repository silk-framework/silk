package de.fuberlin.wiwiss.silk.plugins.dataset.xml

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource, DatasetPlugin}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

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

  override def sink: DataSink = throw new NotImplementedError("XMLs cannot be written at the moment")
}
