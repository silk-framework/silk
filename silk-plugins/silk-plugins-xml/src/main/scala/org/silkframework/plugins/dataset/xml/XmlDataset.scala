package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset._
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.resource.{BulkResource, BulkResourceSupport, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import BulkResourceSupport._

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
case class XmlDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource,
  @Param(value = "The base path when writing XML. For instance: /RootElement/Entity. Should no longer be used for reading XML! Instead, set the base path by specifying it as input type on the subsequent transformation or linking tasks.", advanced = true)
  basePath: String = "",
  @deprecated("This will be removed in the next release.", "")
  @Param(value = "A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements", advanced = true)
  uriPattern: String = "",
  @Param(value = "The output template used for writing XML. Must be valid XML. The generated entity is identified through a processing instruction of the form <?MyEntity?>.")
  outputTemplate: MultilineStringParameter = "<Root><?Entity?></Root>",
  @Param(value = "Streaming allows for reading large XML files.", advanced = true)
  streaming: Boolean = true) extends Dataset with ResourceBasedDataset with BulkResourceSupport {

  validateOutputTemplate()

  val resource = checkIfBulkResource(file)

  override def source(implicit userContext: UserContext): DataSource = {
    if(streaming) {
      new XmlSourceStreaming(resource, basePath, uriPattern)
    } else {
      new XmlSourceInMemory(resource, basePath, uriPattern)
    }
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = throw new NotImplementedError("Links cannot be written at the moment")

  override def entitySink(implicit userContext: UserContext): EntitySink = new XmlSink(resource, outputTemplate.str)

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
    if(procInstructions.size != 1) {
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

  /**
    * Gets called when it is detected that all files in the bulk resource have the different schemata.
    * The implementing class needs to provide a bulk resource object with an input stream that
    * covers all files.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  override def onMultiSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource] =
    throw new UnsupportedOperationException("The xml dataset does not support bulk resources with schema differences" +
      "in its sub resources")

  /**
    * Gets called when it is detected that all files in the bulk resource have the same schema.
    * The implementing class needs to provide a logical concatenation of the individual resources.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  override def onSingleSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource] = {

    val combinedStream = BulkResourceSupport.combineStreams(
      Seq(getXmlElementWrapperInputStreams(GENERATED_XML_ROOT_NAMWE)._1) ++
      bulkResource.inputStreams ++
      Seq(getXmlElementWrapperInputStreams(GENERATED_XML_ROOT_NAMWE)._2),
      None
    )

    Some(BulkResource.createFromBulkResource(bulkResource, combinedStream))

  }

  /**
    * The implementing dataset must provide a way to determine the schema of each resource in the bulk resource.
    * The cardinality of the result is 1, there is only one schema.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  override def checkResourceSchema(bulkResource: BulkResource): Seq[EntitySchema] = {

    val individualSources = for (stream <- bulkResource.inputStreams) yield {
      BulkResource.createFromBulkResource(bulkResource, stream)
    }

    val individualSchemata: IndexedSeq[EntitySchema] = individualSources.map( res => {
      val xmlSource = if(streaming) {
        new XmlSourceStreaming(res, basePath, uriPattern)
      }
      else {
        new XmlSourceInMemory(res, basePath, uriPattern)
      }

      implicit val userContext: UserContext = UserContext.INTERNAL_USER
      val typeUri = xmlSource.retrieveTypes()
      val typedPaths = xmlSource.retrieveTypedPath("")
      EntitySchema(typeUri.head._1, typedPaths)

    }).toIndexedSeq

    getDistinctSchemaDescriptions(individualSchemata)
  }

}
