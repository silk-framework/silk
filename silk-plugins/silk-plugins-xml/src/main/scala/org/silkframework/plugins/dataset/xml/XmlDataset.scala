package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.resource.{BulkResource, BulkResourceBasedDataset, WritableResource}
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}
import scala.xml._

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
case class XmlDataset( @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
                       file: WritableResource,
                       @Param(value = "The base path when writing XML. For instance: /RootElement/Entity. Should no longer be used for reading XML! Instead, set the base path by specifying it as input type on the subsequent transformation or linking tasks.", advanced = true)
                       basePath: String = "",
                       @deprecated("This will be removed in the next release.", "")
                       @Param(value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
                       uriPattern: String = "",
                       @Param(value = "The output template used for writing XML. Must be valid XML. The generated entity is identified through a processing instruction of the form <?MyEntity?>.")
                       outputTemplate: MultilineStringParameter = "<Root><?Entity?></Root>",
                       @Param(value = "Streaming allows for reading large XML files.", advanced = true)
                       streaming: Boolean = true) extends Dataset with BulkResourceBasedDataset {

  validateOutputTemplate()

  override def source(implicit userContext: UserContext): DataSource = {
    if (bulkFile.nonEmpty) {
      bulkSource(bulkFile.get)
    }
    else {
      originalSource
    }
  }

  def bulkSource(bulkResource: BulkResource)(implicit userContext: UserContext): DataSource = {
    if(streaming) {
      new XmlBulkDataSource(bulkResource, basePath, uriPattern)
    }
    else {
      new XmlBulkDataSourceInMemory(bulkResource, basePath, uriPattern)
    }
  }

  def originalSource(implicit userContext: UserContext): DataSource = {
    if(streaming) {
      new XmlSourceStreaming(file, basePath, uriPattern)
    }
    else {
      new XmlSourceInMemory(file, basePath, uriPattern)
    }
  }


  override def linkSink(implicit userContext: UserContext): LinkSink = throw new NotImplementedError("Links cannot be written at the moment")

  override def entitySink(implicit userContext: UserContext): EntitySink = new XmlSink(file, outputTemplate.str)

  /**
    * Validates the output template parameter
    */
  private def validateOutputTemplate(): Unit = {
    val xml = loadString(outputTemplate.str)

    def collectProcInstructions(node: Node): Seq[ProcInstr] = {
      node match {
        case proc: ProcInstr => Seq(proc)
        case _ => node.child.flatMap(collectProcInstructions)
      }
    }

    val procInstructions = collectProcInstructions(xml)
    if (procInstructions.size != 1) {
      throw new ValidationException("outputTemplate must contain exactly one processing intruction of the form <?Entity?> to specify where the entities should be inserted.")
    }
  }

  private def loadString(templateString: String): Elem = {
    // Case 1: input <?Entity?>
    val case1 = Try(XML.loadString(s"<Root>$templateString</Root>"))
    // Case 2: input <Root><?Entity?></Root>
    val case1or2 = case1.orElse(Try(XML.loadString(templateString)))
    case1or2 match {
      case Success(elem) => elem
      case Failure(ex: SAXParseException) =>
        throw new ValidationException("outputTemplate could not be processed as valid XML. Error in line " + ex.getLineNumber + " column " + ex.getColumnNumber)
      case _ =>
        throw new ValidationException("outputTemplate must be valid XML containing a single processing instruction or a single processing " +
          "instruction of the form <?Entity?>!")
    }
  }


}
