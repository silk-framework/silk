package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{WritableResource, Resource}

@Plugin(
  id = "xml",
  label = "XML",
  description =
"""Retrieves all entities from an xml file.""",
  documentation =
"""Typically, this dataset is used to transform an XML file to another format, e.g., to RDF. Currently only reading XML files is supported.
When this dataset is used as an input for another task (e.g., a transformation task), the input type of the consuming task selects the path where the entities to be read are located.

Example:

    <Persons>
      <Person>
        <Name>John Doe</Name>
        <Year>1970</Year>
      </Person>
      <Person>
        <Name>Max Power</Name>
        <Year>1980</Year>
      </Person>
    </Persons>

A transformation for reading all persons of the above XML would set the input type to "/Persons/Person".
The transformation iterates all entities matching the given input path.
"""
)
case class XmlDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: Resource,
  @Param(value = "The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'. If left empty, all direct children of the root element will be read.", advanced = true)
  basePath: String = "",
  @Param(value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
  uriPattern: String = "") extends Dataset {

  override def source: DataSource = new XmlSource(file, basePath, uriPattern)

  override def linkSink: LinkSink = throw new NotImplementedError("XMLs cannot be written at the moment")

  override def entitySink: EntitySink = throw new NotImplementedError("XMLs cannot be written at the moment")

  override def clear() = { }
}
