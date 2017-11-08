package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{WritableResource, Resource}

@Plugin(
  id = "xml",
  label = "XML file",
  description =
"""Retrieves all entities from an xml file.""",
  documentation =
"""Typically, this dataset is used to transform an XML file to another format, e.g., to RDF.
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

A transformation for reading all persons of the above XML would set the input type to `/Person`.
The transformation iterates all entities matching the given input path.
In the above example the first entity to be read is:

    <Person>
      <Name>John Doe</Name>
      <Year>1970</Year>
    </Person>

All paths used in the consuming task are relative to this, e.g., the person name can be addressed with the path `/Name`.

Path examples:

- The empty path selects the root element.
- `/Person` selects all persons.
- `/Person[Year = "1970"]` selects all persons which are born in 1970.
- `/#id` Is a special syntax for generating an id for a selected element. It can be used in URI patterns for entities which do not provide an identifier. Examples: `http://example.org/{#id}` or `http://example.org/{/pathToEntity/#id}`.
- The wildcard * enumerates all direct children, e.g., `/Persons/*/Name`.
- The wildcard ** enumerates all direct and indirect children.
- The backslash can be used to navigate to the parent XML node, e.g., `\Persons/SomeHeader`.
- `#text` retrieves the text of the selected node.
"""
)
case class XmlDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource,
  @Param(value = "The base path when writing XML. For instance: /RootElement/Entity. Should no longer be used for reading XML! Instead, set the base path by specifying it as input type on the subsequent transformation or linking tasks.", advanced = true)
  basePath: String = "",
  @Param(value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
  uriPattern: String = "",
  @Param(value = "The default namespace of the generated XML. All URIs in this namespace will be shortened.", advanced = true)
  defaultNamespace: String = "urn:schema:",
  @Param(value = "Streaming allows for reading large XML files.", advanced = true)
  streaming: Boolean = true) extends Dataset {

  override def source: DataSource = {
    if(streaming) {
      new XmlSourceStreaming(file, basePath, uriPattern)
    } else {
      new XmlSourceInMemory(file, basePath, uriPattern)
    }
  }

  override def linkSink: LinkSink = throw new NotImplementedError("Links cannot be written at the moment")

  override def entitySink: EntitySink = new XmlSink(file, basePath, defaultNamespace)
}
